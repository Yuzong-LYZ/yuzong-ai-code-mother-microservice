package com.yuzong.yuzongaicodemother.core;

import cn.hutool.json.JSONUtil;
import com.yuzong.yuzongaicodemother.ai.AiCodeGeneratorService;
import com.yuzong.yuzongaicodemother.ai.AiCodeGeneratorServiceFactory;
import com.yuzong.yuzongaicodemother.ai.model.HtmlCodeResult;
import com.yuzong.yuzongaicodemother.ai.model.MultiFileCodeResult;
import com.yuzong.yuzongaicodemother.ai.model.message.AiResponseMessage;
import com.yuzong.yuzongaicodemother.ai.model.message.ToolExecutedMessage;
import com.yuzong.yuzongaicodemother.ai.model.message.ToolRequestMessage;
import com.yuzong.yuzongaicodemother.constant.AppConstant;
import com.yuzong.yuzongaicodemother.core.builder.VueProjectBuilder;
import com.yuzong.yuzongaicodemother.core.parser.CodeParserExecutor;
import com.yuzong.yuzongaicodemother.core.saver.CodeFileSaverExecutor;
import com.yuzong.yuzongaicodemother.exception.BusinessException;
import com.yuzong.yuzongaicodemother.exception.ErrorCode;
import com.yuzong.yuzongaicodemother.model.enums.CodeGenTypeEnum;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * AI 代码生成门面类，组合代码生成和保存功能
 * 【门面模式】：就是一个大管家，负责管理所有工人。如：安排司机接我，打扫屋子，做饭等等。然后管家就会自己调用不同的人去做这些事。
 * 旧流程：
 * 1. 这个（类）大管家引入AiCodeGeneratorService实际上就是引入ai模型。
 * 2. 管家手底下其中2个工人，一个是生成html模式的，和生产多文件模式的工人。这两个工人让ai模型辅助工作。
 *     管家安排了另一个工人当这两名工人的头头（也就是统一入口）
 * 总结：说白了还是代码封装，就封装了【我们调用ai模型（工厂模式的产品）生成代码 并调用CodeFileSaver保存文件】 的这个过程。
 *
 * 新流程：
 *  1. 门面模式分别调用代码解析策略执行器 和 文件保存策略执行器 同时引入ai模型
 *  2. 代码解析策略执行器：两种解析类来实现代码解析接口，供执行器调用这两个类。所以可以在门面模式直接调用执行器封装好的解析代码方法
 *  3. 文件保存策略执行器：用一个抽象模版方法来定义保存流程，由不同的2个子类来实现。所以可以在门面模式直接调用执行器封装好的保存文件方法
 *
 * 【已优化】：
 *  1. 本来是有2个统一入口，他们分别有2种保存文件方法。
 *  2. 现在普通的统一入口：保存文件直接调用CodeFileSaverExecutor的保存方法；节省了2个方法的位置。
 *  3. 现在流式统一入口：保存文件用一个通用的方法：processCodeStream，这个方法调用CodeFileSaverExecutor的保存方法。节省了1个方法位置。
 *  4. 为什么流式不能直接调用CodeFileSaverExecutor的保存方法？因为：流式必须要等结束之后才能保存文件，否则会不完整。
 *  5. 所以直接用processCodeStream来时刻收集代码片段，最后保存文件。这个方法是省不了的。一定要有。
 *
 *  最新优化：全部添加了appid参数
 */
@Slf4j
@Service
public class AiCodeGeneratorFacade {

    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;
    @Resource
    private VueProjectBuilder vueProjectBuilder;

    /**
     * 统一入口：根据类型生成并保存代码（使用 appId）
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @return 保存的目录
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        //补充： 根据 appId 获取对应的 aiCodeGeneratorService,如果第一次使用则创建并缓存，确保每个 appId 都有对应的 aiCodeGeneratorService
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, codeGenTypeEnum);
        return switch (codeGenTypeEnum) {
            case HTML -> {
                HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * 统一入口：根据类型生成并保存代码（流式，使用 appId）
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @param appId           应用 ID
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        //补充： 根据 appId 获取对应的 aiCodeGeneratorService,如果第一次使用则创建并缓存，确保每个 appId 都有对应的 aiCodeGeneratorService
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, codeGenTypeEnum);
        return switch (codeGenTypeEnum) {
            case HTML -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            case VUE_PROJECT -> {
                // 修改成TokenStream并优化代码
                TokenStream tokenStream = aiCodeGeneratorService.generateVueProjectCodeStream(appId, userMessage);
                yield processTokenStream(tokenStream, appId);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     *
     * TokenStream 转换为 Flux<String> 方法 json格式
     * 将 TokenStream（AI 模型的流式响应）转换为 Reactor 的 Flux<String> 响应式流
     * 这样前端可以通过 SSE（Server-Sent Events）等方式逐步接收 AI 的回复
     * 备注：此乃模版代码。不需要理解
     *
     * @param tokenStream TokenStream 对象
     * @param appId 应用 ID
     * @return Flux<String> 流式响应
     */
    private Flux<String> processTokenStream(TokenStream tokenStream,Long appId) {
        // Flux.create() = 创建一个"流管道"
        // sink = 管道的入口，你往里面塞什么，前端就能收到什么
        // 返回值 Flux<String> = 这个管道本身（交给 Spring WebFlux 推给前端）
        return Flux.create(sink -> {
            // 现在要监听 TokenStream。
            tokenStream
                    /**
                     * 【事件监听器 ①】监听"AI 正在吐字"事件
                     * 谁触发的？→ LangChain4j 框架底层。
                     * 什么时候触发？→ AI 每吐出一个字/词，就触发一次。
                     *              比如 AI 要回答"你好世界"，就会触发 4 次：
                     *                第1次触发：partialResponse = "你"
                     *                第2次触发：partialResponse = "好"
                     *                第3次触发：partialResponse = "世"
                     *                第4次触发：partialResponse = "界"
                     * partialResponse 从哪来？
                     *                → 框架底层在接收到大模型 API 返回的数据时，
                     *                  自动把文本提取出来，作为参数传进来的。
                     *                  你不需要管它从哪来，框架会"喂"给你。
                     * 这段代码做了什么？
                     *                → 把这个字包装成 AiResponseMessage 信封，
                     *                  然后扔进 sink 管道，推给前端。
                     */
                    .onPartialResponse((String partialResponse) -> {
                        // partialResponse 就是 AI 吐出的那一个字，比如 "你"
                        // 用信封包起来，变成：{"type":"AI_RESPONSE","data":"你"}
                        AiResponseMessage msg = new AiResponseMessage(partialResponse);
                        // 反序列化后，扔进管道，前端就能收到这个 JSON 字符串
                        sink.next(JSONUtil.toJsonStr(msg));
                    })
                    /**
                     * 【事件监听器 ②】监听"AI 想调用工具"事件
                     * 谁触发的？→ 框架底层。
                     * 什么时候触发？→ AI 在思考过程中，决定要调用某个工具时。
                     *   比如 AI 判断用户问的是天气，决定调用 "getWeather" 函数。
                     *   注意：这个事件和上面的 onPartialResponse 是【并列的】，
                     *   不是从 onPartialResponse 传数据过来的！
                     * index 从哪来？
                     *   → 框架传的。AI 可能一次要调用多个工具，
                     *     index 就是第几个工具（0, 1, 2...）。
                     * toolExecutionRequest 从哪来？
                     *   → 框架传的。框架底层收到大模型返回的 JSON，
                     *     已经帮你解析成了 ToolExecutionRequest 对象。
                     *     这个对象里已经有 id、name、arguments 了。
                     * 这段代码做了什么？
                     *   → 把工具调用请求包装成信封，推给前端。
                     *     前端收到后可以显示 "正在查询天气..." 的动画。
                     *
                     */
                    .onPartialToolExecutionRequest((index, toolExecutionRequest) -> {
                        // toolExecutionRequest 是框架给的 Java 对象，不是 JSON 字符串！
                        // 它里面有 .id(), .name(), .arguments() 方法
                        ToolRequestMessage toolRequestMessage = new ToolRequestMessage(toolExecutionRequest);
                        sink.next(JSONUtil.toJsonStr(toolRequestMessage));
                    })

                    /*
                     * 【事件监听器 ③】监听"工具执行完了"事件
                     * 谁触发的？→ 框架底层。
                     * 什么时候触发？→ 你的工具函数执行完毕，返回了结果。
                     *   比如 "getWeather" 函数执行完了，返回 "北京晴天25度"。
                     * toolExecution 从哪来？
                     *   → 框架传的。里面包含了工具的名字、参数、和执行结果。
                     * 这段代码做了什么？
                     *   → 把工具执行结果包装成信封，推给前端。
                     *     前端收到后可以展示一个天气卡片。
                     */
                    .onToolExecuted((ToolExecution toolExecution) -> {
                        // 把原始工具执行结果包装成统一的 ToolExecutedMessage 对象
                        ToolExecutedMessage toolExecutedMessage = new ToolExecutedMessage(toolExecution);
                        sink.next(JSONUtil.toJsonStr(toolExecutedMessage));
                    })

                    /* ④ 完整响应回调：AI 整个回答结束时触发（所有 token 都生成完了）
                     * 这段代码 做了什么？
                     *   → 告诉 sink 管道："数据发完了，可以关管道了"。
                     * 只执行一次
                     */
                    .onCompleteResponse((ChatResponse response) -> {
                        // 执行vue项目构建（同步执行，确保预览时项目已经就绪）
                        String projectPath = AppConstant.CODE_OUTPUT_ROOT_DIR+"/vue_project_"+appId;
                        vueProjectBuilder.buildProject(projectPath);
                        // sink.complete() 表示flux"流结束了"，下游会收到完成信号
                        sink.complete();
                    })

                    // ⑤ 错误回调：过程中发生任何异常时触发
                    .onError((Throwable error) -> {
                        error.printStackTrace();
                        // sink.error() 表示"流出错了"，下游会收到错误信号
                        sink.error(error);
                    })

                    // ⑥ 一切就绪，启动 TokenStream 开始执行！
                    //    不调用 start()，上面的回调永远不会被触发
                    .start();

        });
    }

    /**
     * 通用流式代码处理方法（使用 appId）
     *
     * @param codeStream  代码流
     * @param codeGenType 代码生成类型
     * @param appId       应用 ID
     * @return 流式响应
     */
    private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum codeGenType, Long appId) {
        StringBuilder codeBuilder = new StringBuilder();
        return codeStream.doOnNext(chunk -> {
            // 实时收集代码片段
            codeBuilder.append(chunk);
        }).doOnComplete(() -> {
            // 流式返回完成后保存代码
            try {
                String completeCode = codeBuilder.toString();
                // 使用执行器解析代码
                Object parsedResult = CodeParserExecutor.executeParser(completeCode, codeGenType);
                // 使用执行器保存代码
                File savedDir = CodeFileSaverExecutor.executeSaver(parsedResult, codeGenType, appId);
                log.info("保存成功，路径为：" + savedDir.getAbsolutePath());
            } catch (Exception e) {
                log.error("保存失败: {}", e.getMessage());
            }
        });
    }


}
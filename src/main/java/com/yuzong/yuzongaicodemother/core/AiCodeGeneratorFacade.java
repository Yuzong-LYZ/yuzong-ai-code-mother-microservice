package com.yuzong.yuzongaicodemother.core;

import com.yuzong.yuzongaicodemother.ai.AiCodeGeneratorService;
import com.yuzong.yuzongaicodemother.ai.AiCodeGeneratorServiceFactory;
import com.yuzong.yuzongaicodemother.ai.model.HtmlCodeResult;
import com.yuzong.yuzongaicodemother.ai.model.MultiFileCodeResult;
import com.yuzong.yuzongaicodemother.core.parser.CodeParserExecutor;
import com.yuzong.yuzongaicodemother.core.saver.CodeFileSaverExecutor;
import com.yuzong.yuzongaicodemother.exception.BusinessException;
import com.yuzong.yuzongaicodemother.exception.ErrorCode;
import com.yuzong.yuzongaicodemother.model.enums.CodeGenTypeEnum;
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
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId);
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
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId);

        return switch (codeGenTypeEnum) {
            case HTML -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
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
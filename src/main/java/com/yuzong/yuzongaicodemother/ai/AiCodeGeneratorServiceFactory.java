package com.yuzong.yuzongaicodemother.ai;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.yuzong.yuzongaicodemother.ai.tools.FileWriteTool;
import com.yuzong.yuzongaicodemother.exception.BusinessException;
import com.yuzong.yuzongaicodemother.exception.ErrorCode;
import com.yuzong.yuzongaicodemother.model.enums.CodeGenTypeEnum;
import com.yuzong.yuzongaicodemother.service.ChatHistoryService;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * AI 代码生成服务工厂类
 * 【工厂模式】：核心思想就一句话：你只管用，别管怎么造。---传入 appId，工厂返回对应的 AI 服务实例。
 * 【备注】：实例：任何对象都是实例！普通的new出来的也是实例，动态代理对象也是实例。这里返回的实例实际上是代理对象。
 * 【该类最新逻辑（旧逻辑已经删除，详细看2026/7/6 17:12的代码）】：
 *  1. 需要用到ai服务的类--注入本工厂类，通过 getAiCodeGeneratorService(appId) 获取 AI 服务代理对象
 *  2. 工厂内部使用 ffeine 本地缓存管理代理对象，实现对话记忆的隔离与复用：
 *       - 缓存命中：直接返回已有的 AiCodeGeneratorService 代理对象（保留对话记忆）
 *       - 缓存未命中：自动创建新的AiCodeGeneratorService 代理对象，存入缓存后返回
 *  3. 无论缓存是否命中，调用方始终能拿到一个可用的代理对象
 *  4. 使用代理对象的方法。不过这些方法被我统一包装到AiCodeGeneratorFacade门面类当中。外面的类直接调用门面类即可。
 *  备注：这个代理对象，代理的是：AiCodeGeneratorService接口
 *  备注：将本来的Bean注解删除的情况下，也就是：本类的aiCodeGeneratorService方法删除
 *
 * todo：上述流程已经不适用。因为后续加了别的东西。后续有时间再看看要不要重新写个流程
 */
@Configuration
@Slf4j
public class AiCodeGeneratorServiceFactory {

    /**
     *【注意】当我们注入ChatModel时，就将：在application.yaml配置的信息【ai模型】注入进chatModel里。（包括key，api等）
     * 备注：我们是用deepseek指向用 OpenAI 兼容接口的大模型来定义。模型名：deepseek-v4-flash
     */
    @Resource
    private ChatModel chatModel;


    /**
     * 补充：流式聊天模型
     *  Bug备注：
     *     1. 这里我们@Resource他：【StreamingChatModel streamingChatModel;】== 创建一个 streamingChatModel的容器用来装Bean
     *        备注：这是langchain4j自动配置的。这个Bean也是langchain4j自动创建的。我们只是@Resource他。
     *     2. 我们在ReasoningStreamingChatModelConfig（后续称为A配置类）类中的：
     *        StreamingChatModel reasoningStreamingChatModel()方法上@Bean标签 == 注册并创建了一个reasoningStreamingChatModel的Bean
     *     3. 最终结果：
     *        就本来就有一个Bean，后面我们手动创建了一个Bean，糟糕的是，这两个Bean都是StreamingChatModel类型。
     *        更糟糕的是：现在我们只有这一个StreamingChatModel类型的装Bean的容器。
     *        更加糟糕的是：现在两个Bean的类型一样，但是Bean名不同，且Bean名和现在能装这个类型的Bean容器的名称，没有一个对的上（相同）。
     *        spring不知道到底该将哪个Bean注入到现在这个容器当中。
     *        最终就会【报错】
     *     解决方案：我们将我们这个容器的名称改为：和langchain4j自动创建的Bean名一致。就自动装入langchain4j的那个Bean了。
     *             如何知道Bean名？@Resource左侧有个绿色圆圈，点击就能看到有几个Bean符合。然后选择正确的Bean名。
     *             将容器名改为：openAiStreamingChatModel。即可解决
     */
    @Resource
    private StreamingChatModel openAiStreamingChatModel;

    // 补充：Redis 聊天记忆存储
    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Resource
    private ChatHistoryService chatHistoryService;

    // 将刚刚在配置类创建并注册的Bean注入进来
    @Resource
    private StreamingChatModel reasoningStreamingChatModel;


// 【备注】下面的数字，1. 2. 3. 是执行顺序，并非写的时候的顺序
    /**
     * AI 服务代理对象 本地缓存【这个方法在注入本工厂类时，会自动创建并初始化】
     * 缓存策略：
     * - 最大缓存 1000 个代理对象
     * - 写入后 30 分钟过期
     * - 访问后 10 分钟过期
     */
    private final Cache<String, AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.debug("AI 服务实例被移除，缓存键: {}, 原因: {}", key, cause);
            })
            .build();


    /**
     * 1. 根据 appId 获取服务（带缓存）--根据 appId 获取服务（带缓存）这个方法是为了兼容历史逻辑
     * 如果有appId，则从缓存中获取AiCodeGeneratorService代理对象
     * 如果没有appId，则创建新的AiCodeGeneratorService代理对象存入缓存中
     */
//    public AiCodeGeneratorService getAiCodeGeneratorService(long appId) {
//        return getAiCodeGeneratorService(appId, CodeGenTypeEnum.HTML);
//    }


    /**
     * 根据 appId 和代码生成类型获取服务（带缓存）
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        String cacheKey = buildCacheKey(appId, codeGenType);
        return serviceCache.get(cacheKey, key -> createAiCodeGeneratorService(appId, codeGenType));
    }


    /**
     * 2. 创建新的 AI 服务代理对象
     * 【核心】return部分：
     *        是langchain4j的核心api，他会自动利用 Java 动态代理创建一个代理对象，代理AiCodeGeneratorService接口的代理对象。
     *        这个对象自动实现了 AiCodeGeneratorService接口。因为我们传入了chatModel。所以自带ai模型
     * 【缺点】：他最后返回的是字符串，所以后续需要利用langchain4j的结构化输出。以及将输出结果解析出来保存到本地文件中【已优化】。
     */
    private AiCodeGeneratorService createAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        // 根据 appId 构建独立的对话记忆【隔离不同应用的对话记忆】
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(20)
                .build();
        // 从数据库加载历史对话到记忆中
        chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 20);
        // 根据代码生成类型选择不同的模型配置
        return switch (codeGenType) {
            // Vue 项目生成使用推理模型
            case VUE_PROJECT -> AiServices.builder(AiCodeGeneratorService.class)
                    .streamingChatModel(reasoningStreamingChatModel)
                    .chatMemoryProvider(memoryId -> chatMemory)
                    .tools(new FileWriteTool())
                    // 处理工具调用幻觉问题
                    .hallucinatedToolNameStrategy(toolExecutionRequest -> ToolExecutionResultMessage.from(
                            toolExecutionRequest, "Error: there is no tool called " + toolExecutionRequest.name()
                    ))
                    .build();
            // HTML 和多文件【三件套】生成使用默认模型
            case HTML, MULTI_FILE -> AiServices.builder(AiCodeGeneratorService.class)
                    .chatModel(chatModel)
                    .streamingChatModel(openAiStreamingChatModel)
                    .chatMemory(chatMemory)
                    .build();
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "不支持的代码生成类型: " + codeGenType.getValue());
        };
    }


    /**
     *  构建缓存键
     */
    private String buildCacheKey(long appId, CodeGenTypeEnum codeGenType) {
        return appId + "_" + codeGenType.getValue();
    }

}
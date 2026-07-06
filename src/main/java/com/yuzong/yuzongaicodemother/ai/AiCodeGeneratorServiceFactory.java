package com.yuzong.yuzongaicodemother.ai;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.yuzong.yuzongaicodemother.service.ChatHistoryService;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
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

    // 补充：流式聊天模型
    @Resource
    private StreamingChatModel streamingChatModel;

    // 补充：Redis 聊天记忆存储
    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Resource
    private ChatHistoryService chatHistoryService;

// 【备注】下面的数字，1. 2. 3. 是执行顺序，并非写的时候的顺序
    /**
     * AI 服务代理对象 本地缓存【这个方法在注入本工厂类时，会自动创建并初始化】
     * 缓存策略：
     * - 最大缓存 1000 个代理对象
     * - 写入后 30 分钟过期
     * - 访问后 10 分钟过期
     */
    private final Cache<Long, AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.debug("AI 服务实例被移除，appId: {}, 原因: {}", key, cause);
            })
            .build();


    /**
     * 1. 根据 appId 获取服务（带缓存）
     * 如果有appId，则从缓存中获取AiCodeGeneratorService代理对象
     * 如果没有appId，则创建新的AiCodeGeneratorService代理对象存入缓存中
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId) {
        return serviceCache.get(appId, this::createAiCodeGeneratorService); // 根据appId找缓存中有没有aiservice。有则返回，没有则调用createAiCodeGeneratorService方法创建
    }

    /**
     * 2. 创建新的 AI 服务代理对象
     * 【核心】return部分：
     *        是langchain4j的核心api，他会自动利用 Java 动态代理创建一个代理对象，代理AiCodeGeneratorService接口的代理对象。
     *        这个对象自动实现了 AiCodeGeneratorService接口。因为我们传入了chatModel。所以自带ai模型
     * 【缺点】：他最后返回的是字符串，所以后续需要利用langchain4j的结构化输出。以及将输出结果解析出来保存到本地文件中【已优化】。
     */
    private AiCodeGeneratorService createAiCodeGeneratorService(long appId) {
        log.info("为 appId: {} 创建新的 AI 服务实例", appId);
        // 根据 appId 构建独立的对话记忆【隔离不同应用的对话记忆】
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(20)
                .build();
        // 补充：从数据库加载历史对话到redis记忆中
        chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 20);
        return AiServices.builder(AiCodeGeneratorService.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .chatMemory(chatMemory)
                .build();
    }

}
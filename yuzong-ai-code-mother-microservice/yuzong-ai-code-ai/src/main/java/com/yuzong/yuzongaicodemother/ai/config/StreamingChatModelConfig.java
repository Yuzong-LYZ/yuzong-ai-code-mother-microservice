package com.yuzong.yuzongaicodemother.ai.config;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 * 通用流式聊天模型配置类
 *
 * @author Yuzong
 * @since 2026/7/20 20:10
 */
@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai.streaming-chat-model")
@Data
public class StreamingChatModelConfig {

    /**
     * 基础 URL
     */
    private String baseUrl;

    /**
     * API 密钥
     */
    private String apiKey;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 最大令牌数
     */
    private Integer maxTokens;

    /**
     * 随机参数
     */
    private Double temperature;

    /**
     * 是否记录请求
     */
    private boolean logRequests;

    /**
     * 是否记录响应
     */
    private boolean logResponses;

    /**
     * 流式聊天模型
     */
    @Bean
    @Scope("prototype")  // 加上注解变成多例，是因为你显式地告诉 Spring：“每次有人需要这个 Bean 时，都给我 new 一个新的对象”。
    public StreamingChatModel streamingChatModelPrototype() {
        return OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .maxTokens(maxTokens)
                .temperature(temperature)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .build();
    }
}
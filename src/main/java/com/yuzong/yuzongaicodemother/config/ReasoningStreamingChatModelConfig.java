package com.yuzong.yuzongaicodemother.config;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 * 流式推理模型配置
 * 备注：这里其实和在工厂类（AiCodeGeneratorServiceFactory）配置的是一样的，或者说差不多
 *      但是，如果我们后续需要添加一些新的模型，就可以直接在这里添加。
 *
 */
@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai.reasoning-streaming-chat-model")
@Data
public class ReasoningStreamingChatModelConfig {

    private String baseUrl;

    private String apiKey;

    private String modelName;

    private Integer maxTokens;

    private Double temperature;

    private Boolean logRequests = false;

    private Boolean logResponses = false;

    /**
     * 推理流式模型（用于 Vue 项目生成，带工具调用）--DeepSeek-v4-flash
     * 将这个模型配置成 Bean，方便后续调用。
     * 实际上工厂类那个和这个的功能是一样的，只是工厂类那个Bean是langchain4j自动创建的。而且Deepseek-v4-flash默认打开推理能力。所以一样。
     * 这里完全可以使用pro而不是flash。但是为了省点钱。还是用flash吧。
     * 虽然两个Bean功能上是一样的，但是并非无意义的。后续可以添加一些新的模型。或者可以把这个改成pro模型。如只有会员可以使用pro模型。
     */
    @Bean
    @Scope("prototype")
    public StreamingChatModel reasoningStreamingChatModelPrototype() {
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
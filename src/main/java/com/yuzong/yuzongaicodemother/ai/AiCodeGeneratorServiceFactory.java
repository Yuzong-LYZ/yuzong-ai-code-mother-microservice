package com.yuzong.yuzongaicodemother.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 代码生成服务工厂类
 * 【工厂模式】：核心思想就一句话：你只管用，别管怎么造。--送原材料或者要求去工厂，工厂给你结果。
 *  逻辑：
 *  1. 看下面ai调用逻辑的注释的第三条。
 *  2. 现在我们这个工厂类：AiCodeGeneratorServiceFactory就是工厂本身
 *  3. 而根据1可以知道，我们这个工厂进行了：获取ChatModel大模型，同时进行了一系列操作，最终会返回一个产品回去给别人。
 *  4. 最终的产品实际上就是代理对象：AiCodeGeneratorService。这个产品本身就已经带着ai模型的。
 *  5. 因此，我们别的类想调用ai来用的时候。只需要注入AiCodeGeneratorService即可，调用其方法（他的方法是提示词）即可。
 *  6. 总结：工厂模式，说的很高大上，其实就是代码封装。
 *          获取ai模型，将ai模型交给代理对象，代理对象返回结果给工厂，工厂返回结果给调用者。
 */
@Configuration
public class AiCodeGeneratorServiceFactory {

    /**
     * ai调用逻辑：
     * 1. 刚在application.yaml：指向用 OpenAI 兼容接口的大模型，地址是 DeepSeek 的 API，密钥是 xxx，模型名是 deepseek-chat。
     * 2. 这个chatModel就是自动注入的application的模型
     * 3. 【核心】这个AiCodeGeneratorServiceFactory的aiCodeGeneratorService方法return的方法：
     *    是langchain4j的核心api，他会自动利用 Java 动态代理创建一个对象。
     *    这个对象实现了 AiCodeGeneratorService接口，并使用chatModel大模型
     * 4. 我们要调用ai来用的时候。比如AiCodeGeneratorServiceTest测试类当中，需要注入：AiCodeGeneratorService
     *    因为这个AiCodeGeneratorService看上去是正常注入，但是他的实现类实际上就是langchain4j创建的代理对象，也就是第3点所说的那个代理对象
     *    不过无所谓，实现形式不同而已，把他当作正常的@Resource注入即可
     *    调用时，代理对象自动读取AiCodeGeneratorService类的方法上的 @SystemMessage 注解，
     *    加载提示词文件，将参数作为用户消息，
     *    然后调用内部的 chatModel 发送请求给 DeepSeek 并返回结果。
     * 5. 缺点：他最后返回的是字符串，所以后续需要利用langchain4j的结构化输出。以及将输出结果解析出来保存到本地文件中。
     */
    @Resource
    private ChatModel chatModel;

    /**
     * 创建 AI代码生成器服务
     */
    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService() {
        // 指定AiCodeGeneratorService 接口的，chatModel大模型
        return AiServices.create(AiCodeGeneratorService.class, chatModel);
    }
}
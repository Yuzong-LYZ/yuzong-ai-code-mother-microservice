package com.yuzong.yuzongaicodemother.ai.config;

import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redis 持久化对话记忆存储配置
 *
 * @author yuzong
 * @ date 2023/09/05
 */
@Configuration
@ConfigurationProperties(prefix = "spring.data.redis") // Redis 配置属性读取 application.yml 中 spring.data.redis 的配置
@Data
public class RedisChatMemoryStoreConfig {
    // 这里变量和application.yml中spring.data.redis的配置对应
    private String host;

    private int port;

    private String password;

    private long ttl;


    /**
     * new一个代理对象。RedisChatMemoryStore = 电视。new一个代理对象就 = new一个遥控器。
     * 但是我们在new这个遥控器的时候，需要把电视的配置，蓝牙连接什么的都给遥控器配置好。
     * 然后我们就能通过这个遥控器去控制电视了。
     * 备注：这玩意只是形式看起来有点不眼熟。实际上就是一个实例化对象。RedisChatMemoryStore这个类的实例化对象，仅此而已。
     * 不是什么代理对象之类的。你也可以叫做客户端，也可以叫做遥控器。但是不能说他等同代理和代理对象。不过在不严谨的情况下可以。
     *
     * @return RedisChatMemoryStore 实例化对象
     */
    @Bean
    public RedisChatMemoryStore redisChatMemoryStore() {
        return RedisChatMemoryStore.builder()
                .host(host)
                .port(port)
                .password(password)
                .ttl(ttl)
                .build();
    }
}
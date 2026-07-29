package com.yuzong.yuzongaicodemother.config;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.Resource;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
/**
 * Redis 缓存管理器配置类
 */
@Configuration
public class RedisCacheManagerConfig {

    @Resource // 自动注入 Redis 的连接工厂（Spring Boot 已经帮你配好了连接地址、密码等）
    private RedisConnectionFactory redisConnectionFactory;

    @Bean
    public CacheManager cacheManager() {
        // 配置 ObjectMapper 支持 Java8 时间类型
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        // 默认配置
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30)) // 默认 30 分钟过期
                .disableCachingNullValues() // 禁用 null 值缓存
                // key 使用 String 序列化器
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()));
                // value 使用 JSON 序列化器（支持复杂对象）
                // 但是需要注意：开启后需要给序列化增加默认类型配置，否则无法反序列化
//                .serializeValuesWith(RedisSerializationContext.SerializationPair
//                        .fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper)));
        
        return RedisCacheManager
                .builder(redisConnectionFactory)

                // 把上面定义的默认规则应用上去
                .cacheDefaults(defaultConfig)
                // "good_app_page" 这个缓存不用 30 分钟，只要 5 分钟就过期
                // 比如首页数据变化比较频繁，5分钟刷新一次比较合理
                .withCacheConfiguration("good_app_page",
                        defaultConfig.entryTtl(Duration.ofMinutes(5)))
                .build();    // 构建完成，交出去
    }
}
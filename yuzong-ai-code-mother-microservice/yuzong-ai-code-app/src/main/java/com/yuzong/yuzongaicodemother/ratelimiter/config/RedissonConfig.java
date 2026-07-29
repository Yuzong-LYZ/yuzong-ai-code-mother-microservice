package com.yuzong.yuzongaicodemother.ratelimiter.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson配置类
 * 备注：这就是固定模板，除了set那些需要注意一下，其余都是模版。@Value那些我们早就在application.yml中配置好了，这里只需要把模版写下来就行
 */
@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private Integer redisPort;

    @Value("${spring.data.redis.password}")
    private String redisPassword;

    @Value("${spring.data.redis.database}")
    private Integer redisDatabase;

    /**
     * Redisson客户端，用于操作Redis
     */
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        String address = "redis://" + redisHost + ":" + redisPort;
        SingleServerConfig singleServerConfig = config.useSingleServer()
                .setAddress(address)  // redis地址
                .setDatabase(redisDatabase)  //
                .setConnectionMinimumIdleSize(1)  // 最小空闲连接数
                .setConnectionPoolSize(10)  // 连接池大小
                .setIdleConnectionTimeout(30000)  // 空闲连接超时时间
                .setConnectTimeout(5000)  // 连接超时时间
                .setTimeout(3000)  // 命令执行超时时间
                .setRetryAttempts(3)  // 重试次数
                .setRetryInterval(1500);  // 重试间隔时间
        // 如果有密码则设置密码
        if (redisPassword != null && !redisPassword.isEmpty()) {
            singleServerConfig.setPassword(redisPassword);
        }
        return Redisson.create(config);
    }
}
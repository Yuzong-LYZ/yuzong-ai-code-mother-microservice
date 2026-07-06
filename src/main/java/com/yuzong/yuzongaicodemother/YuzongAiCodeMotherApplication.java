package com.yuzong.yuzongaicodemother;

import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@MapperScan("com.yuzong.yuzongaicodemother.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
@SpringBootApplication(exclude = {RedisEmbeddingStoreAutoConfiguration.class}) // 这里排除 RedisEmbeddingStore 自动配置。否则报错
public class YuzongAiCodeMotherApplication {

    public static void main(String[] args) {
        SpringApplication.run(YuzongAiCodeMotherApplication.class, args);
    }

}

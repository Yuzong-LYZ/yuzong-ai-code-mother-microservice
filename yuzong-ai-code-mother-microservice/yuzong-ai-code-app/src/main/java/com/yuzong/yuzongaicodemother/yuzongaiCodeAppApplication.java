package com.yuzong.yuzongaicodemother;

import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication(exclude = {RedisEmbeddingStoreAutoConfiguration.class})
@MapperScan("com.yuzong.yuzongaicodemother.mapper")
@EnableCaching
public class yuzongaiCodeAppApplication {
    public static void main(String[] args) {
        SpringApplication.run(yuzongaiCodeAppApplication.class, args);
    }
}
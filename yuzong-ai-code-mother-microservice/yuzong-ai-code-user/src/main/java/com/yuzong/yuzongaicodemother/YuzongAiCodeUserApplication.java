package com.yuzong.yuzongaicodemother;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@MapperScan("com.yuzong.yuzongaicodemother.mapper")
@ComponentScan("com.yuzong")
@EnableDubbo
public class YuzongAiCodeUserApplication {
    public static void main(String[] args) {
        SpringApplication.run(YuzongAiCodeUserApplication.class, args);
    }
}
package com.yuzong.yuzongaicodemother;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@MapperScan("com.yuzong.yuzongaicodemother.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
@SpringBootApplication
public class YuzongAiCodeMotherApplication {

    public static void main(String[] args) {
        SpringApplication.run(YuzongAiCodeMotherApplication.class, args);
    }

}

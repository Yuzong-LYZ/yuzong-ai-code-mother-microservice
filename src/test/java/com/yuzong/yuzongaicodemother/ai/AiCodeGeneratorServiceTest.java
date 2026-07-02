package com.yuzong.yuzongaicodemother.ai;

import com.yuzong.yuzongaicodemother.ai.model.HtmlCodeResult;
import com.yuzong.yuzongaicodemother.ai.model.MultiFileCodeResult;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 *
 * @author Yuzong
 * @since 2026/7/2 15:00
 */
@SpringBootTest
class AiCodeGeneratorServiceTest {

    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;

    @Test
    void generateHtmlCode() {
        HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode("做个程序员Yuzong的个人博客，不超过20行");
        Assertions.assertNotNull(result);
    }

    @Test
    void generateMultiFileCode() {
        MultiFileCodeResult multiFileCode = aiCodeGeneratorService.generateMultiFileCode("做个程序员yuzog的留言板，不超过20行");
        Assertions.assertNotNull(multiFileCode);
    }
}
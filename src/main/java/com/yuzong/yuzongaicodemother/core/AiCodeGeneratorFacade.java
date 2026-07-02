package com.yuzong.yuzongaicodemother.core;

import com.yuzong.yuzongaicodemother.ai.AiCodeGeneratorService;
import com.yuzong.yuzongaicodemother.ai.model.HtmlCodeResult;
import com.yuzong.yuzongaicodemother.ai.model.MultiFileCodeResult;
import com.yuzong.yuzongaicodemother.exception.BusinessException;
import com.yuzong.yuzongaicodemother.exception.ErrorCode;
import com.yuzong.yuzongaicodemother.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * AI 代码生成门面类，组合代码生成和保存功能
 * 【门面模式】：就是一个大管家，负责管理所有工人。如：安排司机接我，打扫屋子，做饭等等。然后管家就会自己调用不同的人去做这些事。
 * 1. 这个（类）大管家引入AiCodeGeneratorService实际上就是引入ai模型。
 * 2. 管家手底下其中2个工人，一个是生成html模式的，和生产多文件模式的工人。这两个工人让ai模型辅助工作。
 *     管家安排了另一个工人当这两名工人的头头（也就是统一入口）
 * 总结：说白了还是代码封装，就封装了【我们调用ai模型（工厂模式的产品）生成代码 并调用CodeFileSaver保存文件】 的这个过程。
 */
@Service
public class AiCodeGeneratorFacade {

    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;

    /**
     * 统一入口：根据类型生成并保存代码
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @return 保存的目录
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        return switch (codeGenTypeEnum) {
            case HTML -> generateAndSaveHtmlCode(userMessage);
            case MULTI_FILE -> generateAndSaveMultiFileCode(userMessage);
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * 生成 HTML 模式的代码并保存
     *
     * @param userMessage 用户提示词
     * @return 保存的目录
     */
    private File generateAndSaveHtmlCode(String userMessage) {
        HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode(userMessage);
        return CodeFileSaver.saveHtmlCodeResult(result);
    }

    /**
     * 生成多文件模式的代码并保存
     *
     * @param userMessage 用户提示词
     * @return 保存的目录
     */
    private File generateAndSaveMultiFileCode(String userMessage) {
        MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode(userMessage);
        return CodeFileSaver.saveMultiFileCodeResult(result);
    }
}
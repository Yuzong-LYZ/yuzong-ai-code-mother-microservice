package com.yuzong.yuzongaicodemother.ai.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

/**
 * HTML代码生成结果
 */
@Description("生成Html代码文件结果")  // 引入langchain4j的这个注解，便于ai理解
@Data
public class HtmlCodeResult {

    /**
     * HTML代码
     */
    @Description("HTML代码")  // 引入langchain4j的这个注解，便于ai理解
    private String htmlCode;

    /**
     * 代码描述
     */
    @Description("生成代码的描述")  // 引入langchain4j的这个注解，便于ai理解
    private String description;
}
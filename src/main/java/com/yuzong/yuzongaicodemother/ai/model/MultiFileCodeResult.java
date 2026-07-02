package com.yuzong.yuzongaicodemother.ai.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

/**
 * 多文件代码结果
 */

@Data
@Description("生成多个代码文件的结果")  // 引入langchain4j的这个注解，便于ai理解
public class MultiFileCodeResult {

    /**
     * HTML代码
     */
    @Description("HTML代码")  // 引入langchain4j的这个注解，便于ai理解
    private String htmlCode;

    /**
     * CSS代码
     */
    @Description("CSS代码")  // 引入langchain4j的这个注解，便于ai理解
    private String cssCode;

    /**
     * JavaScript代码
     */
    @Description("JavaScript代码")  // 引入langchain4j的这个注解，便于ai理解
    private String jsCode;

    /**
     * 代码描述
     */
    @Description("生成代码的描述")  // 引入langchain4j的这个注解，便于ai理解
    private String description;
}
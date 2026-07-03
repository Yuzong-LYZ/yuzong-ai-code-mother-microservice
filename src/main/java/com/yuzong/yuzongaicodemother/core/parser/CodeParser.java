package com.yuzong.yuzongaicodemother.core.parser;

/**
 * 代码解析器策略接口
 * 流程：1. 这个接口定义了代码解析器的接口。
 *      2. html和多文件的解析分别实现这个接口。
 *      3. 代码解析执行器分别指向html和多文件的解析器。然后通过传进来不同参数调用不同的解析器。
 *
 * @author yuzong
 */
public interface CodeParser<T> {

    /**
     * 解析代码内容
     * 
     * @param codeContent 原始代码内容
     * @return 解析后的结果对象
     */
    T parseCode(String codeContent);
}

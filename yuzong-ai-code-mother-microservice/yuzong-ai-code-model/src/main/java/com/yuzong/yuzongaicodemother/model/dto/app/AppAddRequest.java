package com.yuzong.yuzongaicodemother.model.dto.app;

import lombok.Data;

import java.io.Serializable;
/**
 * 创建应用请求
 *
 * @author Yuzong
 * @since 2026/7/2 22:44
 */
@Data
public class AppAddRequest implements Serializable {

    /**
     * 应用初始化的 prompt
     */
    private String initPrompt;

    private static final long serialVersionUID = 1L;
}
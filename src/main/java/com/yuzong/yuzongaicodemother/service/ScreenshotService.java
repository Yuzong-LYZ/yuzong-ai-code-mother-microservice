package com.yuzong.yuzongaicodemother.service;

/**
 *
 *
 * @author Yuzong
 * @since 2026/7/12 16:42
 */
public interface ScreenshotService {
    /**
     * 1. 通用的网页截图服务，可以得到截图的访问地址
     *
     * @param webUrl 网页URL
     * @return 截图的访问URL
     */
    String generateAndUploadScreenshot(String webUrl);
}

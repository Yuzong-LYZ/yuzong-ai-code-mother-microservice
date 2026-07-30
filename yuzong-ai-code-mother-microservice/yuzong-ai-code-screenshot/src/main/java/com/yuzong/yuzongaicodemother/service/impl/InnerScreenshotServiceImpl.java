package com.yuzong.yuzongaicodemother.service.impl;

import com.yuzong.yuzongaicodemother.innerservice.InnerScreenshotService;
import com.yuzong.yuzongaicodemother.service.ScreenshotService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * 内部服务实现类
 * 这个类，是提供给别人调用。
 */
@DubboService
@Slf4j
public class InnerScreenshotServiceImpl implements InnerScreenshotService {

    @Resource
    private ScreenshotService screenshotService;

    @Override
    public String generateAndUploadScreenshot(String webUrl) {
        return screenshotService.generateAndUploadScreenshot(webUrl);
    }
}
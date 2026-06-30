package com.yuzong.yuzongaicodemother.controller;

import com.yuzong.yuzongaicodemother.common.BaseResponse;
import com.yuzong.yuzongaicodemother.common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class HealthController {

    @GetMapping("/")
    public BaseResponse<String> healthCheck() {
        return ResultUtils.success( "ok");
    }
}
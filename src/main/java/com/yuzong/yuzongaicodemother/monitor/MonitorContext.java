package com.yuzong.yuzongaicodemother.monitor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 监控上下文（需要传递的数据）-监控上下文的dto请求类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitorContext implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    private String userId;
    private String appId;

}
package com.yuzong.yuzongaicodemother.monitor;

import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.output.TokenUsage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * AI 模型监控监听器 - 自动拦截并收集 AI 调用指标
 * 🤔 这个类是干嘛的？
 * ------------------
 * 这是一个"自动监控摄像头"，它实现了 ChatModelListener 接口，
 * 能够在 AI 模型调用的关键时刻（请求前、响应后、出错时）自动触发，
 * 把收集到的数据交给 AiModelMetricsCollector 去记录。
 */
@Component
@Slf4j
public class AiModelMonitorListener implements ChatModelListener {

    // 用于存储请求开始时间的键
    private static final String REQUEST_START_TIME_KEY = "request_start_time";
    // 用于监控上下文传递（因为请求和响应事件的触发不是同一个线程）
    private static final String MONITOR_CONTEXT_KEY = "monitor_context";

    // 注入：AI 模型指标收集器
    @Resource
    private AiModelMetricsCollector aiModelMetricsCollector;

    /**
     * 【请求前】AI 模型调用开始前的回调
     * 💡 这个方法会做什么？
     * 1. 记录请求开始时间（用于后续计算耗时）
     * 2. 从 MonitorContextHolder 获取当前用户和应用信息
     * 3. 把 MonitorContext 存到 attributes 里（防止跨线程丢失）
     * 4. 记录一次 "started" 状态的请求
     * 🎯 触发时机：
     * - 业务代码调用 chatClient.call() 之后，实际发送 HTTP 请求之前
     *
     * @param requestContext 请求上下文，包含请求参数和 attributes
     */
    @Override
    public void onRequest(ChatModelRequestContext requestContext) {
        // 1. 记录请求开始时间
        requestContext.attributes().put(REQUEST_START_TIME_KEY, Instant.now());

        // 2. 从监控上下文中获取用户和应用信息
        //    MonitorContextHolder 是基于 ThreadLocal 的，存着当前线程的用户信息
        MonitorContext context = MonitorContextHolder.getContext();
        String userId = context.getUserId();
        String appId = context.getAppId();

        // 3. 把 MonitorContext 存到 attributes 里
        //    关键操作！防止 onResponse/onError 在不同线程执行时丢失上下文
        requestContext.attributes().put(MONITOR_CONTEXT_KEY, context);

        // 4. 获取模型名称
        //    从请求对象里拿，比如 deepseek
        String modelName = requestContext.chatRequest().modelName();

        // 5. 记录请求指标
        //    调用 Collector 的 recordRequest 方法，status 传 "started"
        aiModelMetricsCollector.recordRequest(userId, appId, modelName, "started");
    }

    /**
     * 【响应后】AI 模型调用成功后的回调
     * 💡 这个方法会做什么？
     * 1. 从 attributes 里恢复 MonitorContext（防止跨线程丢失）
     * 2. 记录一次 "success" 状态的请求
     * 3. 计算并记录响应耗时
     * 4. 从响应元数据中提取 Token 消耗量并记录
     * 🎯 触发时机：
     * - AI 模型返回成功响应后（HTTP 200 且解析成功）
     *
     * @param responseContext 响应上下文，包含响应结果和 attributes
     */
    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        // 1. 从 attributes 里获取监控信息（由 onRequest 方法存储）
        Map<Object, Object> attributes = responseContext.attributes();

        // 2. 从 attributes 里恢复 MonitorContext
        MonitorContext context = (MonitorContext) attributes.get(MONITOR_CONTEXT_KEY);
        String userId = context.getUserId();
        String appId = context.getAppId();

        // 3. 获取模型名称
        String modelName = responseContext.chatResponse().modelName();

        // 4. 记录成功请求
        // 💡 调用 Collector 的 recordRequest 方法，status 传 "success"
        aiModelMetricsCollector.recordRequest(userId, appId, modelName, "success");

        // 5. 记录响应时间
        // 💡 从 attributes 里取出开始时间，计算耗时
        recordResponseTime(attributes, userId, appId, modelName);

        // 6. 记录 Token 使用情况
        // 💡 从响应元数据里提取 input/output/total Token 数量
        recordTokenUsage(responseContext, userId, appId, modelName);
    }

    /**
     * 【出错时】AI 模型调用失败后的回调
     * 💡 这个方法会做什么？
     * 1. 从 MonitorContextHolder 获取用户和应用信息
     * 2. 记录一次 "error" 状态的请求
     * 3. 记录错误详情（错误消息）
     * 4. 即使是错误，也要记录响应耗时（知道多久后失败的）
     * 🎯 触发时机：
     * - AI 模型调用抛出异常（如网络超时、模型服务不可用、参数错误等）
     * ⚠️ 注意：
     * - 这里用的是 MonitorContextHolder.getContext()，而不是从 attributes 取
     * - 因为 onError 可能发生在请求还没发出去的时候（如参数校验失败）
     * - 这时候 attributes 可能还没初始化，或者没有存 MonitorContext
     *
     * @param errorContext 错误上下文，包含异常信息和 attributes
     */
    @Override
    public void onError(ChatModelErrorContext errorContext) {
        // 1. 从监控上下文中获取用户和应用信息
        // 💡 这里直接用 MonitorContextHolder，因为 onError 可能发生在很早的阶段
        MonitorContext context = MonitorContextHolder.getContext();
        String userId = context.getUserId();
        String appId = context.getAppId();

        // 2. 获取模型名称和错误类型
        // 💡 从请求对象里拿 modelName，从异常对象里拿错误消息
        String modelName = errorContext.chatRequest().modelName();
        String errorMessage = errorContext.error().getMessage();

        // 3. 记录失败请求
        // 💡 调用 Collector 的 recordRequest 方法，status 传 "error"
        aiModelMetricsCollector.recordRequest(userId, appId, modelName, "error");

        // 4. 记录错误详情
        // 💡 调用 Collector 的 recordError 方法，把错误消息也记下来
        aiModelMetricsCollector.recordError(userId, appId, modelName, errorMessage);

        // 5. 记录响应时间（即使是错误响应）
        // 💡 知道多久后失败的也很重要，比如超时错误就能看出来
        Map<Object, Object> attributes = errorContext.attributes();
        recordResponseTime(attributes, userId, appId, modelName);
    }

    /**
     * 记录响应时间
     * 💡 这个方法会做什么？
     * 1. 从 attributes 里取出请求开始时间
     * 2. 用 Instant.now() 获取当前时间
     * 3. 用 Duration.between() 计算耗时
     * 4. 调用 Collector 的 recordResponseTime 方法记录
     *
     * @param attributes 请求属性，包含开始时间
     * @param userId     用户ID
     * @param appId      应用ID
     * @param modelName  模型名称
     */
    private void recordResponseTime(Map<Object, Object> attributes, String userId, String appId, String modelName) {
        // 1. 从 attributes 里取出开始时间
        // 💡 这个时间是 onRequest 时存的 Instant.now()
        Instant startTime = (Instant) attributes.get(REQUEST_START_TIME_KEY);

        // 2. 计算耗时
        // 💡 Duration.between(startTime, endTime) 返回一个 Duration 对象
        // 💡 里面包含了秒数和纳秒数，非常精确
        Duration responseTime = Duration.between(startTime, Instant.now());

        // 3. 记录响应时间
        // 💡 调用 Collector 的 recordResponseTime 方法
        // 💡 Collector 会用 Timer 记录耗时分布（P50、P95、P99 等）
        aiModelMetricsCollector.recordResponseTime(userId, appId, modelName, responseTime);
    }

    /**
     * 记录 Token 使用情况
     * 💡 这个方法会做什么？
     * 1. 从响应元数据里提取 TokenUsage 对象
     * 2. 分别记录 input/output/total 三种 Token 数量
     * 🎯 为什么要记录三种 Token？
     * - input：输入 Token，决定请求成本
     * - output：输出 Token，决定响应成本（通常比 input 贵）
     * - total：总 Token，用于总体统计
     * ⚠️ 注意：
     * - 有些模型可能不返回 TokenUsage（比如本地部署的开源模型）
     * - 所以要判空，避免 NullPointerException
     *
     * @param responseContext 响应上下文
     * @param userId          用户ID
     * @param appId           应用ID
     * @param modelName       模型名称
     */
    private void recordTokenUsage(ChatModelResponseContext responseContext, String userId, String appId, String modelName) {
        // 1. 从响应元数据里提取 TokenUsage
        // 💡 TokenUsage 藏在 chatResponse.metadata 里，业务代码拿不到
        // 💡 只有 Listener 在框架底层才能拿到完整的响应对象
        TokenUsage tokenUsage = responseContext.chatResponse().metadata().tokenUsage();

        // 2. 判空，避免 NullPointerException
        // 💡 有些模型可能不返回 TokenUsage
        if (tokenUsage != null) {
            // 3. 分别记录三种 Token
            // 💡 调用 Collector 的 recordTokenUsage 方法
            // 💡 type 参数传 "input"/"output"/"total"，用于区分不同维度
            aiModelMetricsCollector.recordTokenUsage(userId, appId, modelName, "input", tokenUsage.inputTokenCount());
            aiModelMetricsCollector.recordTokenUsage(userId, appId, modelName, "output", tokenUsage.outputTokenCount());
            aiModelMetricsCollector.recordTokenUsage(userId, appId, modelName, "total", tokenUsage.totalTokenCount());
        }
    }
}
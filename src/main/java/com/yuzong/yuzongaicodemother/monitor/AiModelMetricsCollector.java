package com.yuzong.yuzongaicodemother.monitor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 指标收集器-
 * 🌟 类级别科普：这个类到底是干嘛的？
 * ========================================================================
 * 【核心职责】：它是一个"记账本"，专门负责记录 AI 模型的各种使用数据（指标）。
 * 【工作原理】：
 * 1. 业务代码在调用 AI 时，会调用这个类的方法（比如 recordRequest）来"记账"。
 * 2. 这个类把数据记录到 Micrometer 的指标对象（如 Counter）中。
 * 3. 后台的 Prometheus 监控系统会定期来 MeterRegistry 里"拉取"这些数据。
 * 4. 最终你可以在 Grafana 等面板上看到漂亮的统计图表。
 * 【设计模式】：门面模式/外观模式。它把复杂的 Micrometer API 封装成了简单的业务方法。
 */
@Component
@Slf4j
public class AiModelMetricsCollector {

    /**
     * 【MeterRegistry 是什么？】
     * 它是 Micrometer 的核心，你可以把它理解为"指标管理中心"或"指标数据库"。
     * 所有的指标（Counter、Timer等）最终都要注册（register）到这里，
     * 只有注册到这里，Prometheus 才能采集到它们。
     *
     */
    @Resource
    private MeterRegistry meterRegistry;


    /**
     * 【为什么要搞这4个 Map 缓存？（核心易混淆点！）】--《本地缓存》
     * 错误理解：指标存在 Map 里。
     * 正确理解：指标真正存在 MeterRegistry（管理中心）里！
     * 这4个 Map 只是"本地快捷引用"（相当于你的钱包，MeterRegistry 是银行）。
     * 【为什么需要快捷引用？】
     * 假设用户A请求了10次，我们需要同一个 Counter 对象 +1 十次。
     * 如果每次都去 MeterRegistry 里现找、现创建，性能很差，而且容易创建出重复的指标。
     * 所以我们用 ConcurrentHashMap 把创建好的 Counter 对象缓存起来：
     * 第一次：创建 Counter -> 存入 MeterRegistry -> 把引用放进 Map 缓存。
     * 第二次及以后：直接从 Map 缓存里拿引用 -> 调用 increment() 加 1。
     * 【为什么用 ConcurrentHashMap？】
     * 因为 Web 应用是多线程并发请求的，普通 HashMap 会线程不安全，ConcurrentHashMap 是线程安全的。
     */

    // 缓存"请求次数"的计数器。Key = userId_appId_modelName_status
    private final ConcurrentMap<String, Counter> requestCountersCache = new ConcurrentHashMap<>();
    // 缓存"错误次数"的计数器。Key = userId_appId_modelName_errorMessage
    private final ConcurrentMap<String, Counter> errorCountersCache = new ConcurrentHashMap<>();
    // 缓存"Token消耗"的计数器。Key = userId_appId_modelName_tokenType
    private final ConcurrentMap<String, Counter> tokenCountersCache = new ConcurrentHashMap<>();
    // 缓存"响应时间"的计时器。Key = userId_appId_modelName
    private final ConcurrentMap<String, Timer> responseTimersCache = new ConcurrentHashMap<>();


    /**
     * 记录 AI 请求次数
     *
     * @param userId    用户ID（标签维度1：谁发起的）
     * @param appId     应用ID（标签维度2：哪个应用发起的）
     * @param modelName 模型名称（标签维度3：用的什么模型，如 gpt-4）
     * @param status    状态（标签维度4：成功还是失败，如 success/fail）
     */
    public void recordRequest(String userId, String appId, String modelName, String status) {
        // 1. 拼接缓存的 Key。
        // 为什么要拼接这么多？因为"用户A+成功" 和 "用户B+成功" 应该是两个不同的计数器！
        // 不同的维度组合，对应不同的 Counter 实例。
        String key = String.format("%s_%s_%s_%s", userId, appId, modelName, status);

        // 2. 从缓存中获取 Counter，如果没有就创建一个新的（核心逻辑！）--核心是：computeIfAbsent
        Counter counter = requestCountersCache.computeIfAbsent(key, k -> {
            // 如果缓存里没有这个 key，就会执行下面这段代码来创建新指标：
            return Counter.builder("ai_model_requests_total") // 给指标起个全局唯一的名字（Prometheus 靠这个名字认它）
                    .description("AI模型总请求次数")          // 指标的描述（给人看的）
                    .tag("user_id", userId)                 // 打标签：用户ID
                    .tag("app_id", appId)                   // 打标签：应用ID
                    .tag("model_name", modelName)           // 打标签：模型名称
                    .tag("status", status)                  // 打标签：状态
                    .register(meterRegistry);               // 【关键一步】：把创建好的指标注册到管理中心（银行）里！
        });

        // 3. 计数器 +1。
        // 此时 counter 无论是刚从缓存拿出来的，还是刚创建的，都指向 MeterRegistry 中的真实指标。
        // 调用 increment()，Prometheus 下次来拉数据时，就能看到数值增加了。
        counter.increment();
    }

    /**
     * 记录 AI 调用错误次数
     * 逻辑和 recordRequest 完全一样，只是指标名和标签变了。
     *
     * @param errorMessage 错误信息（作为标签，方便统计哪种错误最多）
     */
    public void recordError(String userId, String appId, String modelName, String errorMessage) {
        String key = String.format("%s_%s_%s_%s", userId, appId, modelName, errorMessage);

        Counter counter = errorCountersCache.computeIfAbsent(key, k ->
                Counter.builder("ai_model_errors_total")
                        .description("AI模型错误次数")
                        .tag("user_id", userId)
                        .tag("app_id", appId)
                        .tag("model_name", modelName)
                        .tag("error_message", errorMessage) // 用错误信息做标签
                        .register(meterRegistry)
        );

        counter.increment(); // 错误次数 +1
    }

    /**
     * 记录 Token 消耗量
     *
     * @param tokenType  Token类型（如：input_tokens, output_tokens, total_tokens）
     * @param tokenCount 本次消耗的具体数量（比如这次消耗了 500 个 token）
     */
    public void recordTokenUsage(String userId, String appId, String modelName,
                                 String tokenType, long tokenCount) {
        String key = String.format("%s_%s_%s_%s", userId, appId, modelName, tokenType);

        Counter counter = tokenCountersCache.computeIfAbsent(key, k ->
                Counter.builder("ai_model_tokens_total")
                        .description("AI模型Token消耗总数")
                        .tag("user_id", userId)
                        .tag("app_id", appId)
                        .tag("model_name", modelName)
                        .tag("token_type", tokenType)
                        .register(meterRegistry)
        );

        // 注意这里！不是 +1，而是 +tokenCount。
        // 比如这次消耗了 500 个 token，计数器就直接加 500。
        counter.increment(tokenCount);
    }

    /**
     * 记录 AI 响应耗时
     * 【Timer 和 Counter 的区别】：
     * Counter 只能做简单的加法（+1, +N）。
     * Timer 不仅能记录"调用了多少次"，还能记录"每次花了多长时间"，
     * 并且能自动计算平均耗时、最大耗时、P99耗时等高级统计数据。
     *
     * @param duration 耗时时长（Java 的 Duration 对象）
     */
    public void recordResponseTime(String userId, String appId, String modelName, Duration duration) {
        // 注意：耗时的 key 没有包含 status 等，因为通常我们只关心"这个模型对这个用户的平均响应时间"
        String key = String.format("%s_%s_%s", userId, appId, modelName);

        Timer timer = responseTimersCache.computeIfAbsent(key, k ->
                Timer.builder("ai_model_response_duration_seconds") // 指标名通常以 _seconds 或 _milliseconds 结尾
                        .description("AI模型响应时间")
                        .tag("user_id", userId)
                        .tag("app_id", appId)
                        .tag("model_name", modelName)
                        .register(meterRegistry)
        );

        // 把这次的耗时记录进去。Timer 内部会自动累加总耗时和总次数。
        timer.record(duration);
    }
}
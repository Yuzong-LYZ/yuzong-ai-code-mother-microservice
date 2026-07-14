package com.yuzong.yuzongaicodemother.core.handler;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.yuzong.yuzongaicodemother.ai.model.message.*;
import com.yuzong.yuzongaicodemother.ai.tools.BaseTool;
import com.yuzong.yuzongaicodemother.ai.tools.ToolManager;
import com.yuzong.yuzongaicodemother.constant.AppConstant;
import com.yuzong.yuzongaicodemother.core.builder.VueProjectBuilder;
import com.yuzong.yuzongaicodemother.model.entity.User;
import com.yuzong.yuzongaicodemother.model.enums.ChatHistoryMessageTypeEnum;
import com.yuzong.yuzongaicodemother.service.ChatHistoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.HashSet;
import java.util.Set;

/**
 * JSON 消息流处理器
 * 处理 VUE_PROJECT 类型的复杂流式响应，包含工具调用信息
 * 备注：之前那个tokenStream 转为 JSON 的方法。但是现在需要将json数据解析出来代码
 *
 */
@Slf4j
@Component
public class JsonMessageStreamHandler {
    @Resource
    private VueProjectBuilder vueProjectBuilder;
    @Resource
    private ToolManager toolManager;

    /**
     * 处理 TokenStream（VUE_PROJECT）
     * 解析 JSON 消息并重组为完整的响应格式
     *
     * @param originFlux         原始流
     * @param chatHistoryService 聊天历史服务
     * @param appId              应用ID
     * @param loginUser          登录用户
     * @return 处理后的流
     */
    public Flux<String> handle(Flux<String> originFlux,
                               ChatHistoryService chatHistoryService,
                               long appId, User loginUser) {
        // 收集数据用于生成后端记忆格式
        StringBuilder chatHistoryStringBuilder = new StringBuilder();
        // 用于跟踪已经见过的工具ID，判断是否是第一次调用
        Set<String> seenToolIds = new HashSet<>();
        return originFlux
                .map(chunk -> {
                    // 1. 解析每个 JSON 消息块
                    return handleJsonMessageChunk(chunk, chatHistoryStringBuilder, seenToolIds);
                })
                .filter(StrUtil::isNotEmpty) // 过滤空字串
                .doOnComplete(() -> {
                    // 流式响应完成后，添加 AI 消息到对话历史
                    String aiResponse = chatHistoryStringBuilder.toString();
                    chatHistoryService.addChatMessage(appId, aiResponse, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                    // 补充：异步构建 Vue 项目
                    String projectPath = AppConstant.CODE_OUTPUT_ROOT_DIR + "/vue_project_" + appId;
                    vueProjectBuilder.buildProjectAsync(projectPath);
                })
                .doOnError(error -> {
                    // 如果AI回复失败，也要记录错误消息
                    String errorMessage = "AI回复失败: " + error.getMessage();
                    chatHistoryService.addChatMessage(appId, errorMessage, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                });
    }

    /**
     * 解析并收集 TokenStream 数据
     */
    private String handleJsonMessageChunk(String chunk, StringBuilder chatHistoryStringBuilder, Set<String> seenToolIds) {
        // 解析 JSON
        // 先将原始 JSON 字符串反序列化成对象
        StreamMessage streamMessage = JSONUtil.toBean(chunk, StreamMessage.class);
        StreamMessageTypeEnum typeEnum = StreamMessageTypeEnum.getEnumByValue(streamMessage.getType());
        switch (typeEnum) {
            case AI_RESPONSE -> {
                // 把 JSON 字符串反序列化成具体的 Java 对象
                AiResponseMessage aiMessage = JSONUtil.toBean(chunk, AiResponseMessage.class);
                String data = aiMessage.getData(); // 抠出里面真正的人类文字（比如"好的，我来帮你"）

                // 动作1：抄送一份到 chatHistoryStringBuilder（为了最后存数据库）
                chatHistoryStringBuilder.append(data);
                // 动作2：直接返回纯文字，顺着流推给前端展示
                return data;
            }
            case TOOL_REQUEST -> {
                ToolRequestMessage toolRequestMessage = JSONUtil.toBean(chunk, ToolRequestMessage.class);
                String toolId = toolRequestMessage.getId(); // 拿到工具id
                String toolName = toolRequestMessage.getName();
                // 检查是否是第一次看到这个工具 ID
                if (toolId != null && !seenToolIds.contains(toolId)) {
                    // 第一次调用这个工具，记录 ID 并完整返回工具信息
                    seenToolIds.add(toolId);
                    BaseTool tool = toolManager.getTool(toolName);
                    return tool.generateToolRequestResponse();
                } else {
                    // 如果这个ID之前已经处理过了（流式传输中同一个请求可能会分多个chunk发过来），直接返回空字符串！
                    return "";
                }
            }
            case TOOL_EXECUTED -> {
                // 1. 解析外层 JSON
                ToolExecutedMessage toolExecutedMessage = JSONUtil.toBean(chunk, ToolExecutedMessage.class);
                String toolName = toolExecutedMessage.getName();
                // 2. 解析内层 JSON（因为 arguments 本身也是一串 JSON 字符串）
                JSONObject jsonObject = JSONUtil.parseObj(toolExecutedMessage.getArguments());
                String relativeFilePath = jsonObject.getStr("relativeFilePath"); // 抠出文件路径（如 src/App.vue）
                String suffix = FileUtil.getSuffix(relativeFilePath);            // 抠出后缀名（如 vue）
                String content = jsonObject.getStr("content");                   // 抠出真正的 Vue 源代码！
                // 根据工具名称获取工具实例并生成相应的结果格式
                BaseTool tool = toolManager.getTool(toolName);
                String result = tool.generateToolExecutedResult(jsonObject);

                // 输出前端和要持久化的内容
                String output = String.format("\n\n%s\n\n", result);

                // 动作1：抄送到chatHistoryStringBuilder（把写好的代码也存进数据库）
                chatHistoryStringBuilder.append(output);
                // 动作2：返回拼装好的 Markdown，推给前端
                return output;
            }
            default -> {
                log.error("不支持的消息类型: {}", typeEnum);
                return "";
            }
        }
    }
}

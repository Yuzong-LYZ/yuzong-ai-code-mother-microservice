package com.yuzong.yuzongaicodemother.core.handler;

import com.yuzong.yuzongaicodemother.model.entity.User;
import com.yuzong.yuzongaicodemother.model.enums.ChatHistoryMessageTypeEnum;
import com.yuzong.yuzongaicodemother.service.ChatHistoryService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * 流处理器 -- 简单文本
 *
 * 处理 HTML 和 MULTI_FILE 类型的流式响应
 */
@Slf4j
public class SimpleTextStreamHandler {

    /**
     * 处理传统流（HTML, MULTI_FILE）
     * 直接收集完整的文本响应
     * 备注：这里的原始流式正常的flux<String>的流，vue项目那个是json格式的。
     *     【实际上这里的流处理器，只是左手倒右手，没有真的处理流。只是顺便记录流的内容保存记录。】
     *     大致流程-ai生成代码-流式代码处理方法里面调用-解析代码（从ai的回答中抠出代码）（同时调用保存代码到路径）-到某个封装前面的代码的统一入口
     *     再到这个方法，这个方法就是将流的内容收集起来，然后保存到数据库中。将流处理后到代码（虽然没处理）返回给前端
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
        StringBuilder aiResponseBuilder = new StringBuilder();
        return originFlux
                .map(chunk -> {
                    // 收集AI响应内容
                    aiResponseBuilder.append(chunk);
                    return chunk;
                })
                .doOnComplete(() -> {
                    // 流式响应完成后，添加AI消息到对话历史
                    String aiResponse = aiResponseBuilder.toString();
                    chatHistoryService.addChatMessage(appId, aiResponse, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                })
                .doOnError(error -> {
                    // 如果AI回复失败，也要记录错误消息
                    String errorMessage = "AI回复失败: " + error.getMessage();
                    chatHistoryService.addChatMessage(appId, errorMessage, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                });
    }
}
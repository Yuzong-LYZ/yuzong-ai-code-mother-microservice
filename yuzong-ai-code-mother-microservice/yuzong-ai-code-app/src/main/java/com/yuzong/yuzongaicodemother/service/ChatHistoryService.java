package com.yuzong.yuzongaicodemother.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.yuzong.yuzongaicodemother.model.dto.chathistory.ChatHistoryQueryRequest;
import com.yuzong.yuzongaicodemother.model.entity.ChatHistory;
import com.yuzong.yuzongaicodemother.model.entity.User;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.time.LocalDateTime;

/**
 * 对话历史 服务层。
 *
 * @author 齐夏：Yuzong，我看到了生生不息的激荡
 */
public interface ChatHistoryService extends IService<ChatHistory> {

    /**
     * 1. 添加对话消息记录
     *
     * @param appId     应用ID
     * @param message   消息内容
     * @param messageType 消息类型
     * @param userId    用户ID
     * @return 是否添加成功
     */
    boolean addChatMessage(Long appId, String message, String messageType, Long userId);

    /**
     * 2. 根据应用ID删除对话消息记录（历史）
     *
     * @param appId 应用ID
     * @return 是否删除成功
     */
    boolean deleteByAppId(Long appId);

    /**
     * 3. 构造查询条件
     *
     * @param chatHistoryQueryRequest 查询条件
     * @return 查询条件
     */
    QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest);

    /**
     * 4. 分页获取app应用的对话历史(游标查询服务方法)
     *
     * @param appId         应用ID
     * @param pageSize      页面大小
     * @param lastCreateTime  lastCreateTime
     * @param loginUser     登录用户
     * @return 对话历史分页列表
     */
    Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                               LocalDateTime lastCreateTime,
                                               User loginUser);

    /**
     * 5.将对话历史从数据库加载到redis中（记忆）中
     * 备注：MessageWindowChatMemory的add不是“加载（Load）”到内存，而是“追加（Add）”到内存，并且“同步写入”到 Redis中
     *
     * @param appId         应用ID
     * @param chatMemory    对话记忆
     * @param maxCount      最大加载条数
     * @return 加载的条数
     */
    int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount);
}

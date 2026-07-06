package com.yuzong.yuzongaicodemother.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.yuzong.yuzongaicodemother.constant.UserConstant;
import com.yuzong.yuzongaicodemother.exception.ErrorCode;
import com.yuzong.yuzongaicodemother.exception.ThrowUtils;
import com.yuzong.yuzongaicodemother.model.dto.chathistory.ChatHistoryQueryRequest;
import com.yuzong.yuzongaicodemother.model.entity.App;
import com.yuzong.yuzongaicodemother.model.entity.ChatHistory;
import com.yuzong.yuzongaicodemother.mapper.ChatHistoryMapper;
import com.yuzong.yuzongaicodemother.model.entity.User;
import com.yuzong.yuzongaicodemother.model.enums.ChatHistoryMessageTypeEnum;
import com.yuzong.yuzongaicodemother.service.AppService;
import com.yuzong.yuzongaicodemother.service.ChatHistoryService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 对话历史 服务层实现。
 *
 * @author 齐夏：Yuzong，我看到了生生不息的激荡
 */
@Service
@Slf4j
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory>  implements ChatHistoryService{
    @Resource
    @Lazy
    private AppService appService;


    /**
     * 1. 添加对话消息记录
     *
     * @param appId     应用ID
     * @param message   消息内容
     * @param messageType 消息类型
     * @param userId    用户ID
     * @return 是否添加成功
     */
    @Override
    public boolean addChatMessage(Long appId, String message, String messageType, Long userId) {
        // 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "消息内容不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(messageType), ErrorCode.PARAMS_ERROR, "消息类型不能为空");
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        // 验证消息类型是否有效
        ChatHistoryMessageTypeEnum messageTypeEnum = ChatHistoryMessageTypeEnum.getEnumByValue(messageType);
        ThrowUtils.throwIf(messageTypeEnum == null, ErrorCode.PARAMS_ERROR, "不支持的消息类型: " + messageType);
        // 插入数据库(严格来说是实体类)，不再是new一个对象，一个个set，而是用builder构造器模式。直接.
        ChatHistory chatHistory = ChatHistory.builder()
                .appId(appId)
                .message(message)
                .messageType(messageType)
                .userId(userId)
                .build();
        // 保存到数据库
        return this.save(chatHistory);
    }

    /**
     * 2. 根据应用ID删除对话消息记录（历史）
     *
     * @param appId 应用ID
     * @return 是否删除成功
     */
    @Override
    public boolean deleteByAppId(Long appId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("appId", appId);
        return this.remove(queryWrapper);
    }

    /**
     * 3. 构造查询条件
     *
     * @param chatHistoryQueryRequest 查询条件
     * @return 查询条件
     */
    @Override
    public QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        if (chatHistoryQueryRequest == null) {
            return queryWrapper;
        }
        Long id = chatHistoryQueryRequest.getId();
        String message = chatHistoryQueryRequest.getMessage();
        String messageType = chatHistoryQueryRequest.getMessageType();
        Long appId = chatHistoryQueryRequest.getAppId();
        Long userId = chatHistoryQueryRequest.getUserId();
        LocalDateTime lastCreateTime = chatHistoryQueryRequest.getLastCreateTime();
        String sortField = chatHistoryQueryRequest.getSortField();
        String sortOrder = chatHistoryQueryRequest.getSortOrder();
        // 拼接查询条件
        queryWrapper.eq("id", id)
                .like("message", message)
                .eq("messageType", messageType)
                .eq("appId", appId)
                .eq("userId", userId);
        // 游标查询逻辑 - 只使用 createTime 作为游标
        if (lastCreateTime != null) {
            queryWrapper.lt("createTime", lastCreateTime);
        }
        // 排序
        if (StrUtil.isNotBlank(sortField)) {
            queryWrapper.orderBy(sortField, "ascend".equals(sortOrder));
        } else {
            // 默认按创建时间降序排列
            queryWrapper.orderBy("createTime", false);
        }
        return queryWrapper;
    }

    /**
     * 4. 分页获取app应用的对话历史(游标查询服务方法)
     *
     * @param appId         应用ID
     * @param pageSize      页面大小
     * @param lastCreateTime  lastCreateTime
     * @param loginUser     登录用户
     * @return 对话历史分页列表
     */
    @Override
    public Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                                      LocalDateTime lastCreateTime,
                                                      User loginUser) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > 50, ErrorCode.PARAMS_ERROR, "页面大小必须在1-50之间");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        // 验证权限：只有应用创建者和管理员可以查看
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        boolean isAdmin = UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());
        boolean isCreator = app.getUserId().equals(loginUser.getId());
        ThrowUtils.throwIf(!isAdmin && !isCreator, ErrorCode.NO_AUTH_ERROR, "无权查看该应用的对话历史");
        // 构建查询条件
        ChatHistoryQueryRequest queryRequest = new ChatHistoryQueryRequest();
        queryRequest.setAppId(appId);
        queryRequest.setLastCreateTime(lastCreateTime);
        QueryWrapper queryWrapper = this.getQueryWrapper(queryRequest);
        // 查询数据
        return this.page(Page.of(1, pageSize), queryWrapper);
    }

    // 新
    /**
     * 5.将对话历史从数据库加载到redis中（记忆）中
     * 备注：MessageWindowChatMemory的add不是“加载（Load）”到内存，而是“追加（Add）”到内存，并且“同步写入”到 Redis中
     *
     * @param appId         应用ID
     * @param chatMemory    对话记忆
     * @param maxCount      最大加载条数
     * @return 加载的条数
     */
    @Override
    public int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount) {
        try {
            // 直接构造查询条件，起始点为 1 而不是 0，忽略最新的用户消息
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .eq(ChatHistory::getAppId, appId)
                    .orderBy(ChatHistory::getCreateTime, false) // 按创建时间降序排列
                    .limit(1, maxCount); // 从第1条开始，最多加载maxCount条数据
            List<ChatHistory> historyList = this.list(queryWrapper);
            if (CollUtil.isEmpty(historyList)) {
                return 0;
            }
            // 查询出来的数据，最新的是第一条，所以需要反转列表
            // 反转列表，确保按时间正序（老的在前，新的在后）
            historyList = historyList.reversed();

//            按时间顺序添加到记忆中
            // 加载成功的总数
            int loadedCount = 0;
            // 先清理历史缓存，防止重复加载
            chatMemory.clear();
            // 遍历列表，将用户消息和AI消息分别添加到记忆中
            for (ChatHistory history : historyList) {
                if (ChatHistoryMessageTypeEnum.USER.getValue().equals(history.getMessageType())) {
                    chatMemory.add(UserMessage.from(history.getMessage()));
                    loadedCount++;
                } else if (ChatHistoryMessageTypeEnum.AI.getValue().equals(history.getMessageType())) {
                    chatMemory.add(AiMessage.from(history.getMessage()));
                    loadedCount++;
                }
            }
            log.info("成功为 appId: {} 加载了 {} 条历史对话", appId, loadedCount);
            return loadedCount; // 返回加载的条数
        } catch (Exception e) {
            log.error("加载历史对话失败，appId: {}, error: {}", appId, e.getMessage(), e);
            // 加载失败不影响系统运行，只是没有历史上下文。所以不能抛异常。
            return 0;
        }
    }
}

package com.yuzong.yuzongaicodemother.langgraph4j.node;
import com.yuzong.yuzongaicodemother.ai.AiCodeGenTypeRoutingService;
import com.yuzong.yuzongaicodemother.ai.AiCodeGenTypeRoutingServiceFactory;
import com.yuzong.yuzongaicodemother.langgraph4j.state.WorkflowContext;
import com.yuzong.yuzongaicodemother.model.enums.CodeGenTypeEnum;
import com.yuzong.yuzongaicodemother.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;
/**
 * 智能路由节点
 * 判断采用哪种类型生成代码，如：html，html三件套，vue。
 */
@Slf4j
public class RouterNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: 智能路由");

            // 补充：智能路由节点逻辑
            CodeGenTypeEnum generationType;
            try {
                // 获取AI路由服务工厂并创建新的路由服务实例
                AiCodeGenTypeRoutingServiceFactory factory = SpringContextUtil.getBean(AiCodeGenTypeRoutingServiceFactory.class);
                AiCodeGenTypeRoutingService routingService = factory.createAiCodeGenTypeRoutingService();
                // 根据原始提示词进行智能路由
                generationType = routingService.routeCodeGenType(context.getOriginalPrompt());
                log.info("AI智能路由完成，选择类型: {} ({})", generationType.getValue(), generationType.getText());
            } catch (Exception e) {
                log.error("AI智能路由失败，使用默认HTML类型: {}", e.getMessage());
                generationType = CodeGenTypeEnum.HTML;
            }

            // 更新状态
            context.setCurrentStep("智能路由");
            context.setGenerationType(generationType);
            return WorkflowContext.saveContext(context);
        });
    }
}
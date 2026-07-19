package com.yuzong.yuzongaicodemother.langgraph4j;
import com.yuzong.yuzongaicodemother.exception.BusinessException;
import com.yuzong.yuzongaicodemother.exception.ErrorCode;
import com.yuzong.yuzongaicodemother.langgraph4j.model.QualityResult;
import com.yuzong.yuzongaicodemother.langgraph4j.node.*;
import com.yuzong.yuzongaicodemother.langgraph4j.state.WorkflowContext;
import com.yuzong.yuzongaicodemother.model.enums.CodeGenTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphRepresentation;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.bsc.langgraph4j.prebuilt.MessagesStateGraph;

import java.util.Map;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;

/**
 * 代码生成工作流（实际可用）
 * 流程
 * 1. 咱们这个类，是整个工作流的组装入口和启动入口。它负责把各个节点注册进去、用边连接起来、编译、然后启动执行。
 * 2. 工作流的节点，是真正执行具体业务逻辑的地方。类似于这个类 = controller，各个节点 = service。
 * 3. 工作流的边，是节点之间的连接。类似于换行或者顿号或者符号之类的。
 * 类似于：你好，我是齐夏。你好就是节点，我是齐夏也是节点。现在我们要求说完你好，就要介绍自己，这句话就是边的意义。你好，我是齐夏中间的符号就是边。
 * 4. 工作流的条件边："如果对方是男的，就说'你好，先生'；如果对方是女的，就说'你好，女士'"。

 * 真流程：
 *   1. 创建工作流的时候，添加节点
 *   2. 创建工作流的时候，定义边的顺序，也就是节点的执行顺序
 *   3. 创建工作流的时候，编译工作流
 *   4. 执行工作流的时候，初始化上下文
 *   5. 执行工作流的时候，启动工作流，会按照创建工作流所定义的边的顺序，依次执行每个节点
 *   6. 执行工作流的时候，获取结果

 * 一些详细说明：
 *   1. 我们的Tool工具类。加 @Component 和 @Tool 这两个注解。前者将类变成Bean。LangChain4j 框架在启动时，会去扫描所有带有 @Tool 注解的方法。
 *      langchain4j会将tool注解和p注解的参数转化为json格式，执行到一些节点类的时候，会调用ai服务（Service），并传入json参数。
 *      于是开始工作了。
 *
 */
@Slf4j
public class CodeGenWorkflow {

    /**
     * 创建完整的工作流
     */
    public CompiledGraph<MessagesState<String>> createWorkflow() {
        try {
            return new MessagesStateGraph<String>()
                    // 添加节点 - 使用完整实现的节点
                    .addNode("image_collector", ImageCollectorNode.create())
                    .addNode("prompt_enhancer", PromptEnhancerNode.create())
                    .addNode("router", RouterNode.create())
                    .addNode("code_generator", CodeGeneratorNode.create())
                    .addNode("project_builder", ProjectBuilderNode.create())
                    .addNode("code_quality_check", CodeQualityCheckNode.create())

                    // 添加边
                    //【解释1】：【普通边】：.addEdge("A", "B") 就是在定义边。它的意思是：“当节点 A 执行完后，自动走向节点 B”。
                    //【解释3】：【条件边】：.addConditionalEdges("节点A","方法B","结果C")。他的意思是：“当节点A执行完后，会调用方法B，并传入结果C”。
                    //    人话：当节点A执行完后，执行方法B，方法B可能会返回2个结果3个结果，结果C回根据不同的结果执行不同的东西。
                    //         比如方法B返回了aaa，方法C他aaa对应的是D节点。整行代码的意思就是：节点A执行完后自动走向节点D。
                    // 【备注】：.addConditionalEdges和edge_async和Map.of是固定搭配。就是固定的外壳。
                    .addEdge(START, "image_collector")
                    .addEdge("image_collector", "prompt_enhancer")
                    .addEdge("prompt_enhancer", "router")
                    .addEdge("router", "code_generator")
                    .addEdge("code_generator", "code_quality_check")
// 新增质检条件边：根据质检结果决定下一步
                    .addConditionalEdges("code_quality_check",
                            edge_async(this::routeAfterQualityCheck),
                            Map.of(
                                    "build", "project_builder",   // 质检通过且需要构建
                                    "skip_build", END,            // 质检通过但跳过构建
                                    "fail", "code_generator"      // 质检失败，重新生成
                            ))
                    .addEdge("project_builder", END)

                    // 编译工作流
                    .compile();
        } catch (GraphStateException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "工作流创建失败");
        }
    }

    /**
     * 执行工作流
     */
    public WorkflowContext executeWorkflow(String originalPrompt) {
        CompiledGraph<MessagesState<String>> workflow = createWorkflow();

        // 初始化 WorkflowContext
        WorkflowContext initialContext = WorkflowContext.builder()
                .originalPrompt(originalPrompt)
                .currentStep("初始化")
                .build();

        GraphRepresentation graph = workflow.getGraph(GraphRepresentation.Type.MERMAID);
        log.info("工作流图:\n{}", graph.content());
        log.info("开始执行代码生成工作流");

        WorkflowContext finalContext = null;
        int stepCounter = 1;
        // 【解释2】：这一步LangGraph4j 框架在底层接管了控制权。它根据上面定义的边（Edge），自动按顺序触发对应的 AsyncNodeAction
        for (NodeOutput<MessagesState<String>> step : workflow.stream(
                Map.of(WorkflowContext.WORKFLOW_CONTEXT_KEY, initialContext))) {
            log.info("--- 第 {} 步完成 ---", stepCounter);
            // 显示当前状态
            WorkflowContext currentContext = WorkflowContext.getContext(step.state());
            if (currentContext != null) {
                finalContext = currentContext;
                log.info("当前步骤上下文: {}", currentContext);
            }
            stepCounter++;
        }
        log.info("代码生成工作流执行完成！");
        return finalContext;
    }


    /**
     * 根据代码生成类型决定是否需要构建
     */
    private String routeBuildOrSkip(MessagesState<String> state) {
        WorkflowContext context = WorkflowContext.getContext(state);
        CodeGenTypeEnum generationType = context.getGenerationType();
        // HTML 和 MULTI_FILE 类型不需要构建，直接结束
        if (generationType == CodeGenTypeEnum.HTML || generationType == CodeGenTypeEnum.MULTI_FILE) {
            return "skip_build";
        }
        // VUE_PROJECT 需要构建
        return "build";
    }
    /**
     * 根据代码质检结果决定下一步
     */
    private String routeAfterQualityCheck(MessagesState<String> state) {
        WorkflowContext context = WorkflowContext.getContext(state);
        QualityResult qualityResult = context.getQualityResult();
        // 如果质检失败，重新生成代码
        if (qualityResult == null || !qualityResult.getIsValid()) {
            log.error("代码质检失败，需要重新生成代码");
            return "fail";
        }
        // 质检通过，使用原有的构建路由逻辑
        log.info("代码质检通过，继续后续流程");
        return routeBuildOrSkip(state);
    }
}
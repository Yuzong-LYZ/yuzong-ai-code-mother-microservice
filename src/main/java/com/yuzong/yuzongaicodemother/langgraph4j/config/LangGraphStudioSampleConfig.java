package com.yuzong.yuzongaicodemother.langgraph4j.config;

import com.yuzong.yuzongaicodemother.langgraph4j.CodeGenWorkflow;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.studio.springboot.AbstractLangGraphStudioConfig;
import org.bsc.langgraph4j.studio.springboot.LangGraphFlow;
import org.springframework.context.annotation.Configuration;

/**
 * 可视化调试配置：（不好用）
 *
 */
@Configuration
public class LangGraphStudioSampleConfig extends AbstractLangGraphStudioConfig {

    final LangGraphFlow flow;

    public LangGraphStudioSampleConfig() throws GraphStateException {
        // 1. 把你想要可视化的工作流创建出来编译后得到状态图
        var workflow = new CodeGenWorkflow().createWorkflow().stateGraph;
        // 2. 得到状态图后，构造一个LangGraphFlow的builder。把图转化为一个LangGraphFlow对象可展示的流程图
        this.flow = LangGraphFlow.builder()
                .title("LangGraph Studio")
                .stateGraph(workflow)
                .build();
    }

    // 3. 重写getFlow方法，返回你构造的LangGraphFlow对象。前端拿到这个对象后，即可展示为流程图。
    @Override
    public LangGraphFlow getFlow() {
        return this.flow;
    }
}
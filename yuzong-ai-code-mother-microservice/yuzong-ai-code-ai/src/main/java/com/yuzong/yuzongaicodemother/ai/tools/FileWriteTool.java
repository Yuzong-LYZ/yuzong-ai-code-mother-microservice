package com.yuzong.yuzongaicodemother.ai.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONObject;
import com.yuzong.yuzongaicodemother.constant.AppConstant;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * 文件写入工具
 * 支持 AI 通过工具调用的方式写入文件
 */
@Slf4j
@Component
public class FileWriteTool extends BaseTool{

    /**
     * 这个langchain4j的Tool注解。--将方法暴露给Ai当工具用--参数为：该工具的名称和描述或说明书
     *                 P注解 -- 给参数提供名称和描述
     *                 ToolMemoryId注解 -- 会自动把你在调ai时传递的appId，自动传递给工具方法的参数中
     *
     * 写入文件到指定路径
     * @param relativeFilePath 文件的相对路径
     * @param content 要写入文件的内容
     */
    @Tool("写入文件到指定路径")
    public String writeFile(
            @P("文件的相对路径")
            String relativeFilePath,
            @P("要写入文件的内容")
            String content,
            @ToolMemoryId Long appId
    ) {
        try {
//         一、构建路径
            // 1. 获取相对路径
            Path path = Paths.get(relativeFilePath);
            // 2. 判断相对路径是否相对路径
            if (!path.isAbsolute()) {
                // 相对路径处理，创建基于 appId 的项目目录
                String projectDirName = "vue_project_" + appId;
                // 获取项目根目录并拼接新的项目名，如：目录/目录/tmp/code_output/项目名
                Path projectRoot = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, projectDirName);
                // 合并相对路径和项目根目录，得到绝对路径，如：目录1/目录2/tmp/code_output/项目名/相对路径某某文件
                path = projectRoot.resolve(relativeFilePath);
            }
//         二、判断我构建的绝对路径的父路径是否存在？不存在则创建父目录
            // 1. 获取父目录：获取的是这个：目录1/目录2/tmp/code_output/项目名
            Path parentDir = path.getParent();
            // 2. 如果父目录不存在则创建父目录
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            }
//         三、写入文件到我指定的路径中
            // 写入文件内容
            Files.write(path, content.getBytes(),
                    StandardOpenOption.CREATE,  // 如果路径内没文件，创一个空文件被下面写入内容
                    StandardOpenOption.TRUNCATE_EXISTING); // 如果文件存在，先清空文件内容再写入。
            log.info("成功写入文件: {}", path.toAbsolutePath());
            // 注意要返回相对路径，不能让 AI 把文件绝对路径返回给用户
            return "文件写入成功: " + relativeFilePath;
        } catch (IOException e) {
            String errorMessage = "文件写入失败: " + relativeFilePath + ", 错误: " + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }

    /**
     * 获取工具英文名称
     * @return 工具名称
     */
    @Override
    public String getToolName() {
        return "writeFile";
    }

    /**
     * 获取工具中文名称
     * @return 工具名称
     */
    @Override
    public String getDisplayName() {
        return "写入文件";
    }

    /**
     * 生成工具调用结果
     * @param arguments 工具调用参数
     * @return 工具调用结果
     */
    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        String relativeFilePath = arguments.getStr("relativeFilePath");
        String suffix = FileUtil.getSuffix(relativeFilePath);
        String content = arguments.getStr("content");
        return String.format("""
                        [工具调用] %s %s
                        ```%s
                        %s
                        ```
                        """, getDisplayName(), relativeFilePath, suffix, content);
    }
}
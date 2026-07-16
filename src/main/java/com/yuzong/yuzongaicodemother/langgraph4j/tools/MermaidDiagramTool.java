package com.yuzong.yuzongaicodemother.langgraph4j.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import com.yuzong.yuzongaicodemother.exception.BusinessException;
import com.yuzong.yuzongaicodemother.exception.ErrorCode;
import com.yuzong.yuzongaicodemother.langgraph4j.model.ImageResource;
import com.yuzong.yuzongaicodemother.langgraph4j.model.enums.ImageCategoryEnum;
import com.yuzong.yuzongaicodemother.manager.CosManager;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Mermaid 架构图生成工具
 * 备注：【mac需要手动配置mmdc路径】。并且需要重写convertMermaidToSvg方法。否则根本获取不到正确的路径。这是idea的问题。
 * 原因：终端：运行mmdc--nvm初始化把node路径加载到Path--输入mmdc执行--env会在path找到node，执行成功
 *      idea：运行mmdc--由于idea的未知原因，连mmdc都找不到
 *                   --用绝对路径运行mmdc（也就是我们配置的路径）--env会在path找node，但是node不在path里，所以失败
 * 解决方案：
 *      1. 在application配置mmdc的绝对路径。绝对路径用命令行：which mmdc查看
 *      2. 在代码中（本类内）手动将路径注入：@Value("${mermaid.mmdc-path:mmdc}")（这里有个回车）private String mmdcPath;
 *      此时就已经可以找到mmdc了，但是还是找不到node。因为node不在path里。
 *      3. 手动将nvm的node路径注入path：env.put("PATH", "/Users/yuzong/.nvm/versions/node/v20.20.2/bin:" + env.get("PATH"));
 */
@Slf4j
@Component
public class MermaidDiagramTool {

    @Resource
    private CosManager cosManager;


    
    @Tool("将 Mermaid 代码转换为架构图图片，用于展示系统结构和技术关系")
    public List<ImageResource> generateMermaidDiagram(@P("Mermaid 图表代码") String mermaidCode,
                                                      @P("架构图描述") String description) {
        if (StrUtil.isBlank(mermaidCode)) {
            return new ArrayList<>();
        }
        try {
            // 转换为SVG图片
            File diagramFile = convertMermaidToSvg(mermaidCode);
            // 上传到COS
            String keyName = String.format("/mermaid/%s/%s",
                    RandomUtil.randomString(5), diagramFile.getName());
            String cosUrl = cosManager.uploadFile(keyName, diagramFile);
            // 清理临时文件
            FileUtil.del(diagramFile);
            if (StrUtil.isNotBlank(cosUrl)) {
                return Collections.singletonList(ImageResource.builder()
                        .category(ImageCategoryEnum.ARCHITECTURE)
                        .description(description)
                        .url(cosUrl)
                        .build());
            }
        } catch (Exception e) {
            log.error("生成架构图失败: {}", e.getMessage(), e);
        }
        return new ArrayList<>();
    }


    @Value("${mermaid.mmdc-path:mmdc}")
    private String mmdcPath;
    /**
     * 将Mermaid代码转换为SVG图片
     */
    private File convertMermaidToSvg(String mermaidCode) {
        // 创建临时输入文件
        File tempInputFile = FileUtil.createTempFile("mermaid_input_", ".mmd", true);
        FileUtil.writeUtf8String(mermaidCode, tempInputFile);
        // 创建临时输出文件
        File tempOutputFile = FileUtil.createTempFile("mermaid_output_", ".svg", true);

        // 根据操作系统选择命令
        String command = SystemUtil.getOsInfo().isWindows() ? "mmdc.cmd" : mmdcPath;

        log.info("准备执行 Mermaid CLI, 命令: {}, 输入: {}, 输出: {}", command, tempInputFile.getAbsolutePath(), tempOutputFile.getAbsolutePath());

        try {
            // 使用 ProcessBuilder 传递数组参数，避免空格和转义问题
            ProcessBuilder processBuilder = new ProcessBuilder(
                    command,
                    "-i", tempInputFile.getAbsolutePath(),
                    "-o", tempOutputFile.getAbsolutePath(),
                    "-b", "transparent"
            );

            // 将错误流重定向到标准输出，方便一起读取
            processBuilder.redirectErrorStream(true);
            Map<String, String> env = processBuilder.environment();
            env.put("PATH", "/Users/yuzong/.nvm/versions/node/v20.20.2/bin:" + env.get("PATH"));
            Process process = processBuilder.start();

            // 读取命令输出
            String output = new String(process.getInputStream().readAllBytes());
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                log.error("Mermaid CLI 执行报错! ExitCode: {}, 输出信息: {}", exitCode, output);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Mermaid CLI 执行失败: " + output);
            }

            log.info("Mermaid CLI 执行成功: {}", output);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用 Mermaid CLI 过程发生异常", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "调用 Mermaid CLI 异常: " + e.getMessage());
        } finally {
            // 清理输入文件
            FileUtil.del(tempInputFile);
        }

        // 检查输出文件
        if (!tempOutputFile.exists() || tempOutputFile.length() == 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Mermaid CLI 执行完毕但未生成文件");
        }

        return tempOutputFile;
    }
//        private File convertMermaidToSvg(String mermaidCode) {
//        // 创建临时输入文件
//        File tempInputFile = FileUtil.createTempFile("mermaid_input_", ".mmd", true);
//        FileUtil.writeUtf8String(mermaidCode, tempInputFile);
//        // 创建临时输出文件
//        File tempOutputFile = FileUtil.createTempFile("mermaid_output_", ".svg", true);
//        // 根据操作系统选择命令
//        // 如果是Windows系统，使用mmdc.cmd，否则使用mmdc
//        String command = SystemUtil.getOsInfo().isWindows() ? "mmdc.cmd" : mmdcPath;
//        // 构建命令
//        String cmdLine = String.format("%s -i %s -o %s -b transparent",
//                command,
//                tempInputFile.getAbsolutePath(),
//                tempOutputFile.getAbsolutePath()
//        );
//        // 执行命令
//        RuntimeUtil.execForStr(cmdLine);
//        // 检查输出文件
//        if (!tempOutputFile.exists() || tempOutputFile.length() == 0) {
//            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Mermaid CLI 执行失败");
//        }
//        // 清理输入文件，保留输出文件供上传使用
//        FileUtil.del(tempInputFile);
//        return tempOutputFile;
//    }
}
package com.yuzong.yuzongaicodemother.core.builder;

import cn.hutool.core.util.RuntimeUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * 构建Vue项目
 * 构建：将代码翻译浏览器能听懂的语言。
 * 备注1：后续如果需要给用户开放手动编辑功能的话。可以在不会输的时候重新构建一次。这时候部署，不重新构建，部署的就是旧的。
 *       但是，预览的页面是旧的。
 * 备注2: 也可以不选择备注1，可以在用户手动编辑后，重新构建一次。但是如果用户多次编辑的话。
 * 备注3: 在部署的时候可以再次构建一次，记得判断是否需要再次构建，因为有可能文件丢失之类的，或者文件夹不在了。可以加个if判断一下。
 *       不存在就重新构建，存在就用以前的。如果懒的话，不管那么多，直接重新构建。
 * 总结：所以至于后续需不需要，开放手动编辑功能。待定。
 *
 * @author Yuzong
 * @since 2026/7/9 14:09
 */
@Slf4j
public class VueProjectBuilder {

    /**
     * 异步构建项目（不阻塞主流程）
     *
     * @param projectPath 项目路径
     */
    public void buildProjectAsync(String projectPath) {
        // 在单独的线程中执行构建，避免阻塞主流程
        // 1. 创建虚拟线程
        Thread.ofVirtual().name("vue-builder-" + System.currentTimeMillis()).start(() -> {
            try {
                // 2. 构建项目
                buildProject(projectPath);
            } catch (Exception e) {
                // 3. 如果异常，不要在异步线程里抛出异常，否则会中断异步线程
                log.error("异步构建 Vue 项目时发生异常: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * 构建 Vue 项目
     *
     * @param projectPath 项目根目录路径
     * @return 是否构建成功
     */
    public boolean buildProject(String projectPath) {
        File projectDir = new File(projectPath);
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            log.error("项目目录不存在: {}", projectPath);
            return false;
        }
        // 检查 package.json 是否存在
        File packageJson = new File(projectDir, "package.json");
        if (!packageJson.exists()) {
            log.error("package.json 文件不存在: {}", packageJson.getAbsolutePath());
            return false;
        }
        log.info("开始构建 Vue 项目: {}", projectPath);
        // 执行 npm install
        if (!executeNpmInstall(projectDir)) {
            log.error("npm install 执行失败");
            return false;
        }
        // 执行 npm run build
        if (!executeNpmBuild(projectDir)) {
            log.error("npm run build 执行失败");
            return false;
        }
        // 验证 dist 目录是否生成
        File distDir = new File(projectDir, "dist");
        if (!distDir.exists()) {
            log.error("构建完成但 dist 目录未生成: {}", distDir.getAbsolutePath());
            return false;
        }
        log.info("Vue 项目构建成功，dist 目录: {}", distDir.getAbsolutePath());
        return true;
    }


    /**
     * 执行 npm install 命令
     */
    private boolean executeNpmInstall(File projectDir) {
        log.info("执行 npm install...");
        // 1. 构建命令
        String command = String.format("%s install", buildCommand("npm"));
        // 2. 调用 executeCommand 方法执行命令
        return executeCommand(projectDir, command, 300); // 5分钟超时
    }

    /**
     * 执行 npm run build 命令
     */
    private boolean executeNpmBuild(File projectDir) {
        log.info("执行 npm run build...");
        // 1. 构建命令
        String command = String.format("%s run build", buildCommand("npm"));
        // 2. 调用 executeCommand 方法执行命令
        return executeCommand(projectDir, command, 180); // 3分钟超时
    }


    /**
     * 判断当前操作系统是否是 Windows
     *
     * @return true 表示是 Windows，false 表示不是 Windows
     */
    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    /**
     * 根据操作系统，构建命令
     *
     * @param baseCommand 基础命令（原来的命令）
     * @return 构建后的命令
     */
    private String buildCommand(String baseCommand) {
        if (isWindows()) {
            return baseCommand + ".cmd";
        }
        return baseCommand;
    }



    /**
     * 通用执行命令 方法
     *
     * @param workingDir     工作目录
     * @param command        命令字符串
     * @param timeoutSeconds 超时时间（秒）
     * @return 是否执行成功
     */
    private boolean executeCommand(File workingDir, String command, int timeoutSeconds) {
        try {

            // 1. 调用java的Runtime去操作系统的终端执行命令
            log.info("在目录 {} 中执行命令: {}", workingDir.getAbsolutePath(), command);
            Process process = RuntimeUtil.exec(
                    null,
                    workingDir, // 在哪个目录执行
                    command.split("\\s+") // 执行什么命令-- 同时命令分割为数组
            );
            // 2. 等待进程完成，设置超时。
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            // 2.1 如果超时，强制终止进程
            if (!finished) {
                log.error("命令执行超时（{}秒），强制终止进程", timeoutSeconds);
                process.destroyForcibly();
                return false;
            }
            // 2.2 检查是否完成命令。如果命令执行成功，则返回true，否则false
            int exitCode = process.exitValue();
            if (exitCode == 0) {
                log.info("命令执行成功: {}", command);
                return true;
            } else {
                log.error("命令执行失败，退出码: {}", exitCode);
                return false;
            }
        } catch (Exception e) {
            log.error("执行命令失败: {}, 错误信息: {}", command, e.getMessage());
            return false;
        }
    }

}
package com.yuzong.yuzongaicodemother.core;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.yuzong.yuzongaicodemother.ai.model.HtmlCodeResult;
import com.yuzong.yuzongaicodemother.ai.model.MultiFileCodeResult;
import com.yuzong.yuzongaicodemother.constant.AppConstant;
import com.yuzong.yuzongaicodemother.model.enums.CodeGenTypeEnum;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * 代码文件保存器
 */
// 废弃；原因：已经优化到文件保存策略执行器
@Deprecated
public class CodeFileSaver {

    // 文件保存根目录
    // 备注：user.dir这个玩意，就相当于固定写法。运行时，会自动获取当前项目的根目录。
    private static final String FILE_SAVE_ROOT_DIR = AppConstant.CODE_OUTPUT_ROOT_DIR;

    /**
     * 保存 HtmlCodeResult
     */
    public static File saveHtmlCodeResult(HtmlCodeResult result) {
        // ① 创建一个唯一的子目录【在根目录下】，目录名带 "html" 标识
        String baseDirPath = buildUniqueDir(CodeGenTypeEnum.HTML.getValue());

        // ② 把 HTML 代码写入 index.html 文件
        writeToFile(baseDirPath, "index.html", result.getHtmlCode());

        // ③ 返回这个目录的 File 对象（方便后续使用，比如打包下载）
        return new File(baseDirPath);
    }

    /**
     * 保存 MultiFileCodeResult
     */
    public static File saveMultiFileCodeResult(MultiFileCodeResult result) {
        // ① 创建一个唯一的子目录【在根目录下】，目录名带 "multi_file" 标识
        String baseDirPath = buildUniqueDir(CodeGenTypeEnum.MULTI_FILE.getValue());

        // ② 分别写入三个文件
        writeToFile(baseDirPath, "index.html", result.getHtmlCode());   // 网页结构
        writeToFile(baseDirPath, "style.css",  result.getCssCode());    // 样式
        writeToFile(baseDirPath, "script.js",  result.getJsCode());     // 脚本逻辑

        // ③ 返回目录
        return new File(baseDirPath);
    }

    /**
     * 构建唯一目录路径：tmp/code_output/bizType_雪花ID【临时文件】
     */
    private static String buildUniqueDir(String bizType) {
        // ① 拼接目录名：业务类型 + 雪花算法ID
        //    例如："html_1928374650123456"
        String uniqueDirName = StrUtil.format("{}_{}", bizType, IdUtil.getSnowflakeNextIdStr());

        // ② 拼接完整路径
        String dirPath = FILE_SAVE_ROOT_DIR + File.separator + uniqueDirName;

        // ③ 创建这个目录（mkdir）
        FileUtil.mkdir(dirPath);

        return dirPath;
    }

    /**
     * 写入单个文件【被上面4个方法调用的】
     */
    private static void writeToFile(String dirPath, String filename, String content) {
        // ① 拼接文件的完整路径
        String filePath = dirPath + File.separator + filename;

        // ② 用 UTF-8 编码把内容写入文件
        FileUtil.writeString(content, filePath, StandardCharsets.UTF_8);
    }
}
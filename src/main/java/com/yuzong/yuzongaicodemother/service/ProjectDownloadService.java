package com.yuzong.yuzongaicodemother.service;

import jakarta.servlet.http.HttpServletResponse;

/**
 *
 *
 * @author Yuzong
 * @since 2026/7/12 18:07
 */
public interface ProjectDownloadService {
    /**
     * 将项目目录打包下载为 zip 文件
     *
     * @param projectPath       项目目录路径
     * @param downloadFileName  下载的文件名（不带.zip后缀）
     * @param response          HTTP 响应对象
     */
    void downloadProjectAsZip(String projectPath, String downloadFileName, HttpServletResponse response);
}

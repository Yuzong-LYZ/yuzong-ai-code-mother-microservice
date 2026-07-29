package com.yuzong.yuzongaicodemother.constant;

/**
 * 应用优先级常量
 *
 * @author Yuzong
 * @since 2026/7/2 22:44
 */
public interface AppConstant {

    /**
     * 精选应用的优先级
     */
    Integer GOOD_APP_PRIORITY = 99;

    /**
     * 默认应用优先级
     */
    Integer DEFAULT_APP_PRIORITY = 0;

    /**
     * 应用生成目录
     */
    String CODE_OUTPUT_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_output";

    /**
     * 应用部署目录
     */
    String CODE_DEPLOY_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_deploy";

    /**
     * 应用部署域名
     * todo：这个域名先用localhost测试，后续再修改为自己的域名
     */
    String CODE_DEPLOY_HOST = "http://localhost";
}
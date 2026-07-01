package com.yuzong.yuzongaicodemother.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户注册请求
 */
//这个类实现Serializable接口，表示这个类可以序列化，给每个类定义一个序列化id
@Data
public class UserRegisterRequest implements Serializable {
//  安装了插件后，alt+insert可以快速生成。mac选择cmd+N
    private static final long serialVersionUID = -6466550039290097037L;
    /**
     * 用户账号,密码，校验密码
     */
    private String userAccount;
    private String userPassword;
    private String checkPassword;
}
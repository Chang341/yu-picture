package com.yupi.yupicturebackend.model.dto.user;

import lombok.Data;

// 用户注册请求
@Data
public class UserRegisterRequest {

    // 账号
    private String userAccount;

    // 密码
    private String userPassword;

    // 确认密码
    private String checkPassword;

}

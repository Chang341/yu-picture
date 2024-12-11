package com.yupi.yupicturebackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.yupicturebackend.model.entity.User;
import com.yupi.yupicturebackend.model.vo.LoginUserVO;
import com.yupi.yupicturebackend.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;

// 用户表「user」相关数据库操作的service服务
public interface UserService extends IService<User> {

    /**
     * 用户注册
     * @Param userAccount   新用户账户
     * @Param userPassword  新用户密码
     * @Param checkPassword 校验密码
     * @Return long         新用户id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户登录
     * @Param userAccount   新用户账户
     * @Param userPassword  新用户密码
     * @Param request       http请求
     * @Return 脱敏后的用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 用户注销
     * @Param request
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获取登录用户
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 获取加密后的密码
     * @Param userPassword 用户密码-明文
     * @Return 密文密码
     */
    String getEncryptedPassword(String userPassword);

    /**
     * 获得脱敏后的登录用户信息
     * @Param user
     * @Return
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 获得脱敏后的用户信息
     * @param user
     * @return
     */
    UserVO getUserVO(User user);
}

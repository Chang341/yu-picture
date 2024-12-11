package com.yupi.yupicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@TableName(value = "user")
@Data
public class User implements Serializable {

    // id
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    // 账号
    private String userAccount;

    // 密码
    private String userPassword;

    // 昵称
    private String userName;

    // 用户头像
    private String userAvatar;

    // 用户简介
    private String userProfile;

    // 用户角色：user/admin
    private String userRole;

    // 编辑时间
    private Date editTime;

    // 创建时间
    private Date createTime;

    // 更新时间
    private Date updateTime;

    // 逻辑删除
    @TableLogic
    private Integer isDelete;

    // 序列化版本号
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}

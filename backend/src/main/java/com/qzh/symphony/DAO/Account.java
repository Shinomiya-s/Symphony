package com.qzh.symphony.DAO;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("account")
public class Account {
    @TableId
    private String userId;
    private String username;
    private String nickname;
    private String password;
    private String email;
    private String phone;
    private String avatarUrl;
    private String bio; //个人简介

    //统计数据
    private Integer mistakeCount;  //错题数
    private Integer aiCount;       //AI数
    private Integer totalConversations;     //累计对话
    private Double knowledgeMastery;        //知识掌握比
    //初始化状态
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic //逻辑删除
    private Integer deleted; // 0-存在, 1-删除

    private LocalDateTime lastLoginTime;

    //偏好设置
    private Boolean emailNotify;    //邮件通知
    private Boolean studyReminder;  //学习提醒
    private Boolean aiSuggestion;   //AI自动建议
    private Boolean mistakePublic;  //公开错题本

    //状态
    private Integer status; // 0-正常, 1-禁用
}

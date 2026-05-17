package com.qzh.symphony.DAO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("public_message")
public class PublicMessage {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String fromUserId;
    private String fromUsername;
    private String content;
    private LocalDateTime createdAt;
}
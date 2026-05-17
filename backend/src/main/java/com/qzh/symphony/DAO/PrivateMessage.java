package com.qzh.symphony.DAO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("private_message")
public class PrivateMessage {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String fromUserId;
    private String toUserId;
    private Integer type;  //0普通消息 1AI整理
    private String content;
    private LocalDateTime createdAt;
    private Integer aiVisible;  //1可见 0不可见
    private String chatId;
}
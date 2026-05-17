package com.qzh.symphony.DAO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_message")
public class AiMessage {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String userId;
    private String aiId;
    private String role;  //"user"或"assistant"
    private String content;
    private LocalDateTime createdAt;
}
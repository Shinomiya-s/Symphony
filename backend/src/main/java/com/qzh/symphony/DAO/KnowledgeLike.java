package com.qzh.symphony.DAO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("knowledge_like")
public class KnowledgeLike {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String knowledgeId;
    private String userId;
    private LocalDateTime createdAt;
}
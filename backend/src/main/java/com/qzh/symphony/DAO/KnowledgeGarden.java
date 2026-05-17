package com.qzh.symphony.DAO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

//知识园区详细单题
@Data
@TableName("knowledge_garden")
public class KnowledgeGarden {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String knowledgePoint;
    private String question;
    private String correctAnswer;
    private String aiAnalysis;
    private String strategy;
    private String tags;
    private Integer weight;
    private String fromUserId;
    private String fromUsername;
    private String sourceMistakeId;
    private LocalDateTime createdAt;
    private String questionHash;
    private Integer likeCount;
}
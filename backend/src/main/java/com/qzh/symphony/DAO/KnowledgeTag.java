package com.qzh.symphony.DAO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("knowledge_tag")
public class KnowledgeTag {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String tagName;
    private Integer questionCount;
    private LocalDateTime createdAt;
}
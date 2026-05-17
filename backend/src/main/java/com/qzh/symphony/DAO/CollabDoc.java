package com.qzh.symphony.DAO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("collab_doc")
public class CollabDoc {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String roomId;
    private String content;
    private LocalDateTime createdAt;
    private String aiSummary;
}
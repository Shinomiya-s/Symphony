package com.qzh.symphony.DAO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("mistake")
public class Mistake {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String userId;
    private String question;
    private String wrongReason;
    private String correctAnswer;
    private String notes;
    private String status;  //待复习、复习中、已掌握
    private Integer retries;
    private Integer isShared;  //0个人 1共享
    private LocalDateTime createdAt;
    private String knowledgePoints;
    private Integer reviewCount;
    private LocalDate nextReviewTime;
    private LocalDateTime statusUpdatedAt;
}
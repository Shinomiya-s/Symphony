package com.qzh.symphony.DAO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("daily_report")
public class DailyReport {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String userId;
    private LocalDate reportDate;
    private String content;
    private Integer isRead;
    private LocalDateTime createdAt;
    private Integer type;   //0为日报,1为推荐
    private String targetUserId;
}
package com.qzh.symphony.DAO;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("friendship_ai")
public class FriendshipAi {
    @TableId
    private String friendshipId;
    private String aiName;
    private LocalDateTime createdAt;
}
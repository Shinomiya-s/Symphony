package com.qzh.symphony.DAO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("friend_request")
public class FriendRequest {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String fromUserId;
    private String toUserId;
    private Integer status;
    private Date createdAt;
}

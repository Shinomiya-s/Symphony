package com.qzh.symphony.DAO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("friendship")
public class Friendship {
    @TableId(type = IdType.INPUT)
    private String id;
    private String userId;
    private String friendId;
    private Integer aiEnabled;
    private Date createdAt;
}
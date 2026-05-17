package com.qzh.symphony.DAO;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("tree_tag")
public class TreeTag {
    private String id;
    private String tagName;
    private String parentId;
    private Integer level;  //层级深度
    private Integer sortOrder;  //排列顺序
}
package com.qzh.symphony.DAO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.qzh.symphony.config.Enum.AiType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor  //给mp的无参构造
@AllArgsConstructor
@TableName("ai_assistant")
public class AiAssistant {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String name;
    private String description;
    private String prompt;
    private String avatar;
    private AiType type;          //"selfCreate"或"EnglishPartner"或"algorithmAssistant"
    private String ownerId;         //用户ID,默认AI为-1
    private Integer dialogCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

package com.qzh.symphony.config.Enum;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AiType {
    ALGORITHM_ASSISTANT("algorithmAssistant"),   //算法小助手
    ENGLISH_PARTNER("EnglishPartner"),            //英语练习伙伴
    SELF_CREATE("selfCreate");                    //自定义AI

    @EnumValue  //将这个值存入数据库
    @JsonValue  //Jackson序列化时使用这个值
    private final String value;

    AiType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
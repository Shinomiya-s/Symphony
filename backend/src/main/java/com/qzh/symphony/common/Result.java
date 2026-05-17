package com.qzh.symphony.common;

import lombok.Data;

@Data
public class Result<T> {
    private Integer code;    //状态码：200 成功，500 失败
    private String msg;      //提示消息
    private T data;          //核心数据
    //成功时的静态快捷方法
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMsg("操作成功");
        result.setData(data);
        return result;
    }

    //失败时的静态快捷方法
    public static <T> Result<T> error(String msg) {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMsg(msg);
        return result;
    }
}

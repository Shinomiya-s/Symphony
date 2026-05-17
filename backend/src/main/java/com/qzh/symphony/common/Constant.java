package com.qzh.symphony.common;

public class Constant {
    //Redis缓存配置常量
    public static final String CACHE_KEY_PREFIX = "chat:messages:";
    //分布式锁的 key 前缀
    public static final String LOCK_KEY_PREFIX = "chat:lock:";
    public static final int INITIAL_LOAD_SIZE = 50;
    public static final int MORE_LOAD_SIZE = 100;
    public static final int MAX_CACHE_SIZE = 500;
    public static final long CACHE_EXPIRE_HOURS = 24;
}

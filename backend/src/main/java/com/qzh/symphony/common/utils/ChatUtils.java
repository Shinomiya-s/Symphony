package com.qzh.symphony.common.utils;

public class ChatUtils {
    //便捷方法,用于创建好友的chatId,返回String
    public static String buildFriendShipId(String uid1, String uid2) {
        return uid1.compareTo(uid2) < 0 ? uid1 + "_" + uid2 : uid2 + "_" + uid1;
    }
}

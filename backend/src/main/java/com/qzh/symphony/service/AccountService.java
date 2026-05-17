package com.qzh.symphony.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qzh.symphony.DAO.Account;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
public interface AccountService extends IService<Account> {
    //用户登录
    Map<String, Object> login(String username, String password);

    //用户注册
    boolean register(Account account);


    //获取用户画像
    Account getAccountProfile(String userId);
    Long getFriendCount(String userId);
    //更新用户偏好设置
    boolean updatePreferences(String userId, Account preferences);
    Map<String, Object> getUserProfile(String username, String currentUserId);

    Integer getMistakeCount(String userId);
    Integer getTotalConversations(String userId);
    Double getKnowledgeMastery(String userId);
}

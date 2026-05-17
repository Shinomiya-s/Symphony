package com.qzh.symphony.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qzh.symphony.DAO.AiAssistant;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface AiAssistantService extends IService<AiAssistant>{
    //创建个人AI
    Boolean createPersonalAi(AiAssistant aiAssistant);
    //创建群聊AI
    AiAssistant createGroupAi(Long groupId, String name, String description);
    //获取用户的个人AI列表
    List<AiAssistant> getUserPersonalAis(String userId);
    //获取群聊的AI列表(废弃)
    List<AiAssistant> getGroupAis(Long groupId);
    //增加对话次数(废弃)
    void incrementDialogCount(String aiId);
    //统计用户AI数量(废弃)
    Long countUserAis(String userId);
    //删除AI
    boolean deleteAi(String aiId, String currentUserId);
    //更新AI
    boolean updatePersonalAi(AiAssistant aiAssistant,String userId);
}

package com.qzh.symphony.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qzh.symphony.DAO.Account;
import com.qzh.symphony.DAO.AiAssistant;
import com.qzh.symphony.config.Enum.AiType;
import com.qzh.symphony.mapper.AiAssistantMapper;
import com.qzh.symphony.service.AccountService;
import com.qzh.symphony.service.AiAssistantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AiAssistantServiceimpl extends ServiceImpl<AiAssistantMapper, AiAssistant> implements AiAssistantService {
    @Autowired
    AccountServiceImpl accountServiceImpl;

    //创建私有自定义AI
    @Override
    public Boolean createPersonalAi(AiAssistant aiAssistant) {
        String ownerId=aiAssistant.getOwnerId();
        if(baseMapper.selectOne(new LambdaQueryWrapper<AiAssistant>()
                .eq(AiAssistant::getOwnerId, ownerId)
                .eq(AiAssistant::getType, AiType.SELF_CREATE))!=null)   //每个用户只能创建一个自定义AI
            return false;
        aiAssistant.setId(UUID.randomUUID().toString());
        aiAssistant.setType(AiType.SELF_CREATE);
        aiAssistant.setDialogCount(0);
        aiAssistant.setCreatedAt(LocalDateTime.now());
        aiAssistant.setUpdatedAt(LocalDateTime.now());
        boolean inserted = baseMapper.insert(aiAssistant) > 0;
        if (inserted) {
            return accountServiceImpl.update(new UpdateWrapper<Account>()
                    .lambda()
                    .eq(Account::getUserId, ownerId)
                    .setSql("ai_count = ai_count + 1"));
        }
        return false;
    }

    //废弃方法
    @Override
    public AiAssistant createGroupAi(Long groupId, String name, String description) {
        return null;
    }

    //获取AI列表
    @Override
    public List<AiAssistant> getUserPersonalAis(String userId) {
        List<AiAssistant> allAis=new ArrayList<>();
        //手动构造第一个默认AI:算法小助手
        AiAssistant algoAi = new AiAssistant();
        algoAi.setId("1"); //固定 ID,方便前端判断isDefaultAI
        algoAi.setName("算法小助手");
        algoAi.setAvatar("🤖");
        algoAi.setDescription("专注于算法和数据结构学习，能够帮助分析错题、生成练习题");
        algoAi.setPrompt("你是一个专业的算法导师，擅长数据结构和算法题目的讲解。");
        algoAi.setType(AiType.ALGORITHM_ASSISTANT);
        algoAi.setOwnerId("SYSTEM"); //标记为系统拥有
        algoAi.setDialogCount(0);
        algoAi.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        allAis.add(algoAi);

        //手动构造第二个默认AI：英语练习伙伴
        AiAssistant englishAi = new AiAssistant();
        englishAi.setId("2");
        englishAi.setName("英语练习伙伴");
        englishAi.setAvatar("📚");
        englishAi.setDescription("日常英语对话练习，纠正发音和语法错误");
        englishAi.setPrompt("你是一个友好的英语教练，帮助用户提升英语会话能力。");
        englishAi.setType(AiType.ENGLISH_PARTNER);
        englishAi.setOwnerId("SYSTEM");
        englishAi.setDialogCount(0);
        englishAi.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        allAis.add(englishAi);
        List<AiAssistant> userAis = baseMapper.selectList(new LambdaQueryWrapper<AiAssistant>()
                .eq(AiAssistant::getOwnerId, userId)
                .eq(AiAssistant::getType, AiType.SELF_CREATE)
        );
        if (userAis != null) {
            allAis.addAll(userAis);
        }
        return allAis;
    }

    //废弃方法
    @Override
    public List<AiAssistant> getGroupAis(Long groupId) {
        return List.of();
    }

    //废弃方法
    @Override
    public void incrementDialogCount(String aiId) {
        baseMapper.update(null, new UpdateWrapper<AiAssistant>()
                .eq("id", aiId)
                .setSql("dialog_count = dialog_count + 1"));
    }

    //废弃方法
    @Override
    public Long countUserAis(String userId) {
        return baseMapper.selectCount(new LambdaQueryWrapper<AiAssistant>()
                .eq(AiAssistant::getOwnerId, userId)
                .eq(AiAssistant::getType, AiType.SELF_CREATE));
    }

    //删除AI
    @Override
    public boolean deleteAi(String aiId, String currentUserId) {
        LambdaQueryWrapper<AiAssistant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiAssistant::getId, aiId)
                .eq(AiAssistant::getOwnerId, currentUserId); //必须是自己的才能删

        boolean deleted = baseMapper.delete(wrapper)>0;
        if (deleted) {
            return accountServiceImpl.update(new UpdateWrapper<Account>()
                    .lambda()
                    .eq(Account::getUserId, currentUserId)
                    .setSql("ai_count = ai_count - 1"));
        }
        return false;
    }

    //更新AI
    @Override
    public boolean updatePersonalAi(AiAssistant aiAssistant, String userId) {
        AiAssistant existingAi = baseMapper.selectOne(new LambdaQueryWrapper<AiAssistant>()
                .eq(AiAssistant::getId, aiAssistant.getId())
                .eq(AiAssistant::getOwnerId, userId)                //归属校验
                .eq(AiAssistant::getType, AiType.SELF_CREATE));
        if (existingAi != null) {
            existingAi.setName(aiAssistant.getName());
            existingAi.setDescription(aiAssistant.getDescription());
            existingAi.setPrompt(aiAssistant.getPrompt());
            existingAi.setUpdatedAt(LocalDateTime.now());
            return baseMapper.updateById(existingAi) > 0;
        }
        return false;
    }
}

package com.qzh.symphony.controller;

import com.qzh.symphony.DAO.AiAssistant;
import com.qzh.symphony.common.Result;
import com.qzh.symphony.service.impl.AiAssistantServiceimpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class AiAssistantController {
    @Autowired
    private AiAssistantServiceimpl aiAssistantServiceimpl;

    //创建AI
    @PostMapping("/createPersonalAi")
    public Result createPeronalAi(@RequestBody AiAssistant aiAssistant) {
        aiAssistant.setCreatedAt(LocalDateTime.now());
        aiAssistant.setDialogCount(0);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Result.error("未登录");
        }
        String userId = (String) authentication.getPrincipal();
        aiAssistant.setOwnerId(userId);
        if(aiAssistantServiceimpl.createPersonalAi(aiAssistant)) {
            return Result.success("success");
        }
        return Result.error("fail");
    }

    //删除AI
    @PostMapping("/delete/{id}")
    public Result deleteAi(@PathVariable("id") String id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Result.error("未登录");
        }
        String userId = (String) authentication.getPrincipal();
        if (aiAssistantServiceimpl.deleteAi(id, userId)) {
            return Result.success("删除成功");
        }
        return Result.error("删除失败");
    }

    //查询AI列表
    @GetMapping("/listPersonalAis/{id}")
    public List<AiAssistant> listPersonalAis(@PathVariable("id") String userId) {
        return aiAssistantServiceimpl.getUserPersonalAis(userId);
    }

    //更新AI
    @PostMapping("/updateAi")
    public Result updateAi(@RequestBody AiAssistant aiAssistant) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Result.error("未登录");
        }
        String userId = (String) authentication.getPrincipal();
        if(aiAssistantServiceimpl.updatePersonalAi(aiAssistant,userId)) {
            return Result.success("success");
        }
        return Result.error("fail");
    }
}

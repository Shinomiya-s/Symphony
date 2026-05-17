package com.qzh.symphony.controller;

import com.qzh.symphony.DAO.Account;
import com.qzh.symphony.common.Result;
import com.qzh.symphony.common.utils.JwtUtils;
import com.qzh.symphony.service.impl.AccountServiceImpl;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class LoginController {
    @Autowired
    private AccountServiceImpl accountService;
    @Autowired
    private JwtUtils jwtUtils;

    //登录
    @PostMapping("/login")
    public Result login(@RequestBody Map<String, String> loginData) {
        String phone = loginData.get("phone");
        String password = loginData.get("password");
        //基本校验
        if (StringUtils.isBlank(phone) || StringUtils.isBlank(password)) {
            return Result.error("电话或密码不能为空");
        }
        try {
            //再交给service处理
            Map<String, Object> data = accountService.login(phone, password);
            //登录成功返回的data里包含token
            return Result.success(data);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    //注册
    @PostMapping("/register")
    public Result register(@RequestBody Account account) {
        boolean success = accountService.register(account);
        return success ? Result.success("注册成功") : Result.error("注册失败");
    }

    //获取个人资料
    @GetMapping("/profile")
    public Result profile(@RequestHeader("Authorization") String token) {
        String userId = jwtUtils.getUserIdFromToken(token.replace("Bearer ", ""));
        Account account = accountService.getAccountProfile(userId);
        if (account == null) return Result.error("用户不存在");
        //基本信息
        Map<String, Object> map = new HashMap<>();
        map.put("userId", account.getUserId());
        map.put("username", account.getUsername());
        map.put("nickname", account.getNickname());
        map.put("email", account.getEmail());
        map.put("phone", account.getPhone());
        map.put("bio", account.getBio());
        map.put("createdAt", account.getCreatedAt());
        //统计数据
        map.put("mistakeCount", accountService.getMistakeCount(userId));
        map.put("totalConversations", accountService.getTotalConversations(userId));
        map.put("knowledgeMastery", accountService.getKnowledgeMastery(userId));
        map.put("aiCount", account.getAiCount() != null ? account.getAiCount() : 0);
        map.put("friendCount", accountService.getFriendCount(userId));
        //偏好设置,且空值兜底
        map.put("emailNotify", account.getEmailNotify() != null ? account.getEmailNotify() : true);
        map.put("studyReminder", account.getStudyReminder() != null ? account.getStudyReminder() : true);
        map.put("aiSuggestion", account.getAiSuggestion() != null ? account.getAiSuggestion() : false);
        map.put("mistakePublic", account.getMistakePublic() != null ? account.getMistakePublic() : false);

        return Result.success(map);
    }

    //更新资料
    @PostMapping("/update")
    public Result update(@RequestBody Account account) {
        //从SecurityContext拿当前用户id，用于防止用户篡改别人的资料。
        Object principal=SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String currentUserId=principal.toString();
        boolean success = accountService.updatePreferences(currentUserId, account);
        if (success) {
            return Result.success("资料更新成功！");
        } else {
            return Result.error("更新失败，请重试");
        }
    }
}

package com.qzh.symphony.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qzh.symphony.DAO.Account;
import com.qzh.symphony.DAO.DailyReport;
import com.qzh.symphony.common.Result;
import com.qzh.symphony.common.utils.DifyUtils;
import com.qzh.symphony.common.utils.JwtUtils;
import com.qzh.symphony.mapper.AccountMapper;
import com.qzh.symphony.mapper.DailyReportMapper;
import com.qzh.symphony.service.MistakeAiService;
import com.qzh.symphony.service.impl.MistakeAiServiceImpl;
import com.qzh.symphony.task.DailyReportTask;
import io.jsonwebtoken.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/report")
public class ReportController {

    @Autowired
    private DailyReportMapper dailyReportMapper;
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    MistakeAiServiceImpl mistakeAiService;
    @Autowired
    private DailyReportTask dailyReportTask;
    @Autowired
    private DifyUtils difyUtils;
    @Autowired
    private AccountMapper accountMapper;

    //获取昨天的日报
    @GetMapping("/today")
    public Result getTodayReport(@RequestHeader("Authorization") String authHeader) {
        String userId = jwtUtils.getUserIdFromToken(authHeader.replace("Bearer ", ""));

        List<DailyReport> reports = dailyReportMapper.selectList(
                new LambdaQueryWrapper<DailyReport>()
                        .eq(DailyReport::getReportDate, LocalDate.now().minusDays(1))   //昨天的
                        .eq(DailyReport::getType, 0)    //type为0的日报
                        .eq(DailyReport::getIsRead, 0)  //只查询未读的
                        .eq(DailyReport::getUserId, userId)  //只查自己那条
        );

        if (reports.isEmpty()) return Result.success(null);

        List<Map<String, Object>> result = reports.stream().map(r -> {
            String otherId = r.getUserId().equals(userId) ? r.getTargetUserId() : r.getUserId();
            Account other = accountMapper.selectOne(
                    new LambdaQueryWrapper<Account>().eq(Account::getUserId, otherId)
            );
            Map<String, Object> item = new HashMap<>();
            item.put("id", r.getId());
            item.put("targetId", otherId);
            item.put("targetName", other != null ? other.getUsername() : otherId);
            item.put("summary", r.getContent());
            return item;
        }).collect(Collectors.toList());

        return Result.success(result);
    }

    //标记已读
    @PutMapping("/{id}/read")
    public Result markRead(@PathVariable String id) {
        DailyReport report = new DailyReport();
        report.setId(id);
        report.setIsRead(1);
        dailyReportMapper.updateById(report);
        return Result.success("success");
    }

    //测试用的接口,用于测试手动触发生成日报
    @GetMapping("/generate")
    public Result manualGenerate() throws IOException {
        dailyReportTask.generateDailyReports();
        return Result.success("生成完毕");
    }

    //推荐逻辑:用户若昨天生成日报且对话过该用户后的第二天在点击开与另一位用户的私聊时触发推荐逻辑
    @GetMapping("/recommend")
    public Result getRecommend(@RequestHeader("Authorization") String authHeader,
                               @RequestParam String targetUserId) throws java.io.IOException {
        String userId = jwtUtils.getUserIdFromToken(authHeader.replace("Bearer ", ""));
        String[] pair = normalizePair(userId, targetUserId);

        //查今天自己那条是否已生成
        DailyReport existing = dailyReportMapper.selectOne(
                new LambdaQueryWrapper<DailyReport>()
                        .eq(DailyReport::getUserId, userId)
                        .eq(DailyReport::getTargetUserId, targetUserId)
                        .eq(DailyReport::getReportDate, LocalDate.now())
                        .eq(DailyReport::getType, 1)
        );
        //查询到就返回缓存的
        if (existing != null) {
            Map<String, Object> res = new HashMap<>();
            res.put("id", existing.getId());
            res.put("content", existing.getContent());
            return Result.success(res);
        }

        //查昨天的日报
        DailyReport report = dailyReportMapper.selectOne(
                new LambdaQueryWrapper<DailyReport>()
                        .eq(DailyReport::getUserId, pair[0])
                        .eq(DailyReport::getTargetUserId, pair[1])
                        .eq(DailyReport::getReportDate, LocalDate.now().minusDays(1))
                        .eq(DailyReport::getType, 0)
        );
        if (report == null) return Result.success(null);

        //把日报内容喂给 Dify，生成学习建议
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("chatMode", "chatRecommend");
        String recommend = difyUtils.callDify(inputs, report.getContent(), pair[0] + "_" + pair[1]);

        //给两个用户各插一条记录,且内容相同，但is_read各自独立
        DailyReport rec = new DailyReport();
        rec.setUserId(pair[0]);
        rec.setTargetUserId(pair[1]);
        rec.setReportDate(LocalDate.now());
        rec.setContent(recommend);
        rec.setType(1);
        rec.setIsRead(0);
        rec.setCreatedAt(LocalDateTime.now());
        dailyReportMapper.insert(rec);

        DailyReport rec2 = new DailyReport();
        rec2.setUserId(pair[1]);
        rec2.setTargetUserId(pair[0]);
        rec2.setReportDate(LocalDate.now());
        rec2.setContent(recommend);
        rec2.setType(1);
        rec2.setIsRead(0);
        rec2.setCreatedAt(LocalDateTime.now());
        dailyReportMapper.insert(rec2);

        //返回当前用户那条
        DailyReport myRec = userId.equals(pair[0]) ? rec : rec2;
        Map<String, Object> res = new HashMap<>();
        res.put("id", myRec.getId());
        res.put("content", myRec.getContent());
        return Result.success(res);
    }

    //规范化用户对,返回String[]
    private String[] normalizePair(String a, String b) {
        return a.compareTo(b) <= 0 ? new String[]{a, b} : new String[]{b, a};
    }
}
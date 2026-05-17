package com.qzh.symphony.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.qzh.symphony.DAO.DailyReport;
import com.qzh.symphony.DAO.Mistake;
import com.qzh.symphony.common.Result;
import com.qzh.symphony.common.utils.JwtUtils;
import com.qzh.symphony.mapper.DailyReportMapper;
import com.qzh.symphony.mapper.MistakeMapper;
import com.qzh.symphony.service.MistakeAiService;
import com.qzh.symphony.task.DailyReportTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/mistakes")
public class MistakeController {
    @Autowired
    MistakeMapper mistakeMapper;
    @Autowired
    MistakeAiService mistakeAiService;
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Autowired
    JwtUtils jwtUtils;
    @Autowired
    DailyReportTask  dailyReportTask;

    //查询错题列表(自己的/公用的)
    @GetMapping("/list")
    public Result list(@RequestParam(defaultValue = "personal") String view) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        List<Mistake> mistakes;
        if ("shared".equals(view)) {
            mistakes = mistakeMapper.selectList(
                    new LambdaQueryWrapper<Mistake>()
                            .eq(Mistake::getIsShared, 1)
                            .orderByDesc(Mistake::getCreatedAt)
            );
        } else {
            mistakes = mistakeMapper.selectList(
                    new LambdaQueryWrapper<Mistake>()
                            .eq(Mistake::getUserId, userId)
                            .orderByDesc(Mistake::getCreatedAt)
            );
        }
        return Result.success(mistakes);
    }

    // 添加错题
    @PostMapping("/add")
    public Result add(@RequestBody Mistake mistake) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        mistake.setUserId(userId);
        mistake.setStatus("待复习");
        mistake.setRetries(0);
        mistake.setIsShared(0);
        mistake.setCreatedAt(LocalDateTime.now());
        mistake.setReviewCount(0);
        mistake.setNextReviewTime(LocalDate.now().plusDays(1));
        mistakeMapper.insert(mistake);
        mistakeAiService.extractKnowledgePoints(mistake.getId(), mistake.getQuestion(), mistake.getWrongReason()); //异步提取知识点
        stringRedisTemplate.delete("mistake:analysis:" + userId);  //清理分析缓存
        return Result.success(mistake);
    }

    // 删除错题
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable String id) {
        mistakeMapper.deleteById(id);
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        stringRedisTemplate.delete("mistake:analysis:" + userId);  //清理分析缓存
        return Result.success("success");
    }

    //编辑错题
    @PutMapping("/{id}")
    public Result update(@PathVariable String id, @RequestBody Mistake mistake) {
        mistake.setId(id);

        LambdaUpdateWrapper<Mistake> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Mistake::getId, id);
        if (mistake.getWrongReason() != null) {
            wrapper.set(Mistake::getWrongReason, mistake.getWrongReason());
        }
        if (mistake.getCorrectAnswer() != null) {
            wrapper.set(Mistake::getCorrectAnswer, mistake.getCorrectAnswer());
        }
        if (mistake.getIsShared() != null) {
            wrapper.set(Mistake::getIsShared, mistake.getIsShared());
        }
        mistakeMapper.update(null, wrapper);

        // 清缓存保留，但不再调 Dify
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        stringRedisTemplate.delete("mistake:analysis:" + userId);

        return Result.success("success");
    }

    //AI分析错题
    @GetMapping("/analysis")
    public Result analysis() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            return Result.success(mistakeAiService.analyzeKnowledgePoints(userId));
        } catch (IOException e) {
            return Result.error(e.getMessage());
        }
    }

    //生成练习题
    @PostMapping("/practice")
    public Result<?> generatePractice(@RequestBody Map<String, Object> body) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        List<String> weakPoints = (List<String>) body.get("weakPoints");
        try {
            List<Map<String, String>> questions = mistakeAiService.generatePractice(userId, weakPoints);
            return Result.success(questions);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    //复习计划:查今天需要复习的错题，基于遗忘曲线
    @GetMapping("/review/today")
    public Result getTodayReview(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtUtils.getUserIdFromToken(token);
        List<Mistake> list = mistakeAiService.getTodayReview(userId);
        return Result.success(list);
    }

    //复习计划: 更新复习状态，同时推进下次复习时间
    @PutMapping("/{id}/status")
    public Result updateStatus(@PathVariable String id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        Mistake old = mistakeMapper.selectById(id);
        Mistake update = new Mistake();
        update.setId(id);
        update.setStatus(status);
        //记录状态更新时间
        update.setStatusUpdatedAt(LocalDateTime.now());

        if (old.getNextReviewTime() == null || !old.getNextReviewTime().isAfter(LocalDate.now())) {
            int newCount = old.getReviewCount() + 1;
            update.setReviewCount(newCount);
            update.setNextReviewTime(mistakeAiService.calcNextReviewTime(newCount));
        }
        mistakeMapper.updateById(update);
        return Result.success("success");
    }

    //协作功能:找两个用户的共同薄弱知识点
    @GetMapping("/common-points")
    public Result getCommonPoints(@RequestParam String userId1, @RequestParam String userId2) {
        //每天只查一次，结果缓存到 Redis
        String today = LocalDate.now().toString(); // 例如 "2026-03-08"
        String smaller = userId1.compareTo(userId2) < 0 ? userId1 : userId2;
        String bigger  = userId1.compareTo(userId2) < 0 ? userId2 : userId1;
        String redisKey = "common_points:" + smaller + "_" + bigger + ":" + today;

        //今天已经查过，直接返回空（用于不再提示）
        if (stringRedisTemplate.hasKey(redisKey)) {
            return Result.success(Collections.emptyList());
        }

        List<Mistake> list1 = mistakeMapper.selectList(
                new LambdaQueryWrapper<Mistake>().eq(Mistake::getUserId, userId1)
        );
        List<Mistake> list2 = mistakeMapper.selectList(
                new LambdaQueryWrapper<Mistake>().eq(Mistake::getUserId, userId2)
        );

        Set<String> set1 = list1.stream()
                .filter(m -> m.getKnowledgePoints() != null)
                .flatMap(m -> Arrays.stream(m.getKnowledgePoints().split(",")))
                .map(String::trim)
                .filter(s -> !s.isEmpty() && !s.equals("无"))
                .collect(Collectors.toSet());

        Set<String> set2 = list2.stream()
                .filter(m -> m.getKnowledgePoints() != null)
                .flatMap(m -> Arrays.stream(m.getKnowledgePoints().split(",")))
                .map(String::trim)
                .filter(s -> !s.isEmpty() && !s.equals("无"))
                .collect(Collectors.toSet());

        set1.retainAll(set2);
        if (!set1.isEmpty()) {
            //有共同错题才记录，TTL到今天结束
            long secondsUntilMidnight = LocalDateTime.now()
                    .until(LocalDate.now().plusDays(1).atStartOfDay(), java.time.temporal.ChronoUnit.SECONDS);
            stringRedisTemplate.opsForValue().set(redisKey, "1", Duration.ofSeconds(secondsUntilMidnight));
        }
        return Result.success(set1);
    }

    //协作功能:取完AI给出的学习建议后立刻删掉，防止重复弹出
    @GetMapping("/suggest")
    public Result getSuggest(@RequestParam String targetUserId) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        String pairKey = userId.compareTo(targetUserId) < 0
                ? userId + "_" + targetUserId
                : targetUserId + "_" + userId;
        String key = "mistake:suggest:" + userId + ":" + pairKey;
        String val = stringRedisTemplate.opsForValue().get(key);
        if (val == null) return Result.success(null);
        stringRedisTemplate.delete(key);  // 取完删掉避免重复弹
        return Result.success(val);
    }
}
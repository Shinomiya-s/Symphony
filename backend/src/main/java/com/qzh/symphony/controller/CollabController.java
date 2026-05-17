package com.qzh.symphony.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qzh.symphony.DAO.CollabDoc;
import com.qzh.symphony.common.Result;
import com.qzh.symphony.mapper.CollabDocMapper;
import com.qzh.symphony.service.CollabAiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/collab")
public class CollabController {
    @Autowired
    private CollabDocMapper collabDocMapper;
    @Autowired
    private CollabAiService collabAiService;

    //保存文档
    @PostMapping("/save")
    public Result saveMarkdown(@RequestBody Map<String, String> body) {
        String roomId = body.get("roomId");
        String content = body.get("content");
        CollabDoc doc = new CollabDoc();
        doc.setRoomId(roomId);
        doc.setContent(content);
        doc.setCreatedAt(LocalDateTime.now());
        collabDocMapper.insert(doc);
        //异步调用AI生成摘要
        collabAiService.summarizeAsync(doc.getId(), content);
        return Result.success("success");
    }

    //获取文档列表
    @GetMapping("/{roomId}/list")
    public Result list(@PathVariable("roomId") String roomId) {
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!roomId.contains(currentUserId)) {
            return Result.error("无权访问");
        }
        //鉴权,格式为userId1_userId2,且包含当前用户的id才能访问
        List<CollabDoc> docs = collabDocMapper.selectList(
                Wrappers.<CollabDoc>lambdaQuery()
                        .eq(CollabDoc::getRoomId, roomId)
                        .orderByDesc(CollabDoc::getCreatedAt)
        );
        return Result.success(docs);
    }

    //搜索文档,三个可选参数
    @GetMapping("/{roomId}/search")
    public Result search(
            @PathVariable("roomId") String roomId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "time") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {

        try {
            String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
            if (!roomId.contains(currentUserId)) {
                return Result.error("无权访问");
            }
            LambdaQueryWrapper<CollabDoc> queryWrapper = new LambdaQueryWrapper<>();

            //必须属于该会话
            queryWrapper.eq(CollabDoc::getRoomId, roomId);

            //关键词搜索(内容 OR AI摘要)
            if (keyword != null && !keyword.trim().isEmpty()) {
                queryWrapper.and(wrapper -> wrapper
                        .like(CollabDoc::getContent, keyword)
                        .or()
                        .like(CollabDoc::getAiSummary, keyword)
                );
            }

            //排序
            if ("time".equals(sortBy)) {
                if ("asc".equals(sortOrder)) {
                    queryWrapper.orderByAsc(CollabDoc::getCreatedAt);
                } else {
                    queryWrapper.orderByDesc(CollabDoc::getCreatedAt);
                }
            } else if ("length".equals(sortBy)) {
                //按内容长度排序
                if ("asc".equals(sortOrder)) {
                    queryWrapper.last("ORDER BY LENGTH(content) ASC");
                } else {
                    queryWrapper.last("ORDER BY LENGTH(content) DESC");
                }
            }

            List<CollabDoc> results = collabDocMapper.selectList(queryWrapper);
            return Result.success(results);

        } catch (Exception e) {
            return Result.error("搜索失败: " + e.getMessage());
        }
    }

}
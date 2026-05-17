package com.qzh.symphony.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qzh.symphony.DAO.CollabDoc;
import com.qzh.symphony.common.utils.DifyUtils;
import com.qzh.symphony.mapper.CollabDocMapper;
import com.qzh.symphony.service.CollabAiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Async("aiTaskExecutor")
public class CollabAiServiceImpl implements CollabAiService {
    @Autowired
    private DifyUtils difyUtils;
    @Autowired
    private CollabDocMapper collabDocMapper;

    private final ObjectMapper mapper = new ObjectMapper();

    //异步生成摘要
    @Async("aiTaskExecutor")
    public void summarizeAsync(String docId, String content) {
        try {
            Map<String, Object> inputs = new HashMap<>();
            inputs.put("chatMode", "collabSummary");
            inputs.put("userId", docId);
            String result = difyUtils.callDify(inputs, content, docId);
            if (result != null && !result.isEmpty()) {
                CollabDoc doc = new CollabDoc();
                doc.setId(docId);
                doc.setAiSummary(result);   //只更新摘要字段
                collabDocMapper.updateById(doc);
            }
        } catch (Exception e) {
            System.err.println("协作文档总结失败: " + e.getMessage());
        }
    }
}

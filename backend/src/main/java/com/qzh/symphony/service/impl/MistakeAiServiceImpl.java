package com.qzh.symphony.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qzh.symphony.DAO.KnowledgeTag;
import com.qzh.symphony.DAO.Mistake;
import com.qzh.symphony.common.utils.DifyUtils;
import com.qzh.symphony.mapper.*;
import com.qzh.symphony.service.KnowledgeGardenService;
import com.qzh.symphony.service.MistakeAiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class MistakeAiServiceImpl implements MistakeAiService {
    @Autowired
    MistakeMapper mistakeMapper;
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Autowired
    DifyUtils difyUtils;
    @Autowired
    KnowledgeGardenMapper knowledgeGardenMapper;
    @Autowired
    AccountMapper accountMapper;
    @Autowired
    KnowledgeTagMapper knowledgeTagMapper;
    @Autowired
    TreeTagMapper treeTagMapper;
    @Autowired
    KnowledgeGardenService knowledgeGardenService;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${dify.api.url}")
    private String difyApiUrl;
    @Value("${dify.api.key}")
    private String difyApiKey;

    //提取知识点,异步执行
    @Override
    @Async("aiTaskExecutor")
    public void extractKnowledgePoints(String mistakeId, String question, String wrongReason) {
        try {
            //查所有现有tag
            List<KnowledgeTag> existingTags = knowledgeTagMapper.selectList(null);
            String tagList = existingTags.stream()
                    .map(KnowledgeTag::getTagName)
                    .collect(Collectors.joining("、"));
            //早期设计Java内部拼prompt
            String query = "错题：" + question + "\n原因：" + wrongReason + "\n\n" +
                    "当前已有知识点标签库：" + (tagList.isEmpty() ? "（暂无）" : tagList) + "\n" +
                    "请提取该错题涉及的知识点。如果与已有标签语义相近，直接使用已有标签名称；只有完全不属于任何已有标签时才新增。" +
                    "返回格式：用逗号分隔的标签名，例如：导数,函数极值";
            Map<String, Object> inputs = new HashMap<>();
            inputs.put("chatMode", "knowledgeReview");
            inputs.put("userId", mistakeId);
            String result=difyUtils.callDify(inputs,query,mistakeId);
            //Dify返回标签，比如"导数,函数极值"
            if (result.length() > 0) {
                Mistake mistake = new Mistake();
                mistake.setId(mistakeId);
                String cleaned = result.replaceAll("知识点[:：]", "").trim();
                String[] tagArray = cleaned.split(",");
                for (String tag : tagArray) {
                    //逐个标签处理：已有的questionCount+1，没有的新建
                    String tagName = tag.trim();
                    if (tagName.isEmpty() || tagName.equals("无")) continue;
                    KnowledgeTag existing = knowledgeTagMapper.selectOne(
                            new LambdaQueryWrapper<KnowledgeTag>()
                                    .eq(KnowledgeTag::getTagName, tagName)
                    );
                    if (existing == null) {
                        KnowledgeTag newTag = new KnowledgeTag();
                        newTag.setTagName(tagName);
                        newTag.setQuestionCount(1);
                        newTag.setCreatedAt(LocalDateTime.now());
                        knowledgeTagMapper.insert(newTag);
                    } else {
                        existing.setQuestionCount(existing.getQuestionCount() + 1);
                        knowledgeTagMapper.updateById(existing);
                    }
                }
                //更新mistake的knowledgePoints字段
                mistake.setKnowledgePoints(cleaned);
                mistakeMapper.updateById(mistake);
                Mistake fullMistake = mistakeMapper.selectById(mistakeId);
                //触发知识园区生成
                knowledgeGardenService.asyncGenerateKnowledgeGarden(fullMistake, fullMistake.getUserId());
            }
        } catch (Exception e) {
            System.err.println("知识点提取失败: " + e.getMessage());
        }
    }

    @Override
    public List<Mistake> findByUserId(String userId) {
        List<Mistake> list = mistakeMapper.selectList(Wrappers.<Mistake>lambdaQuery().eq(Mistake::getUserId, userId));
        return list;
    }

    //分析薄弱知识点
    @Override
    public Map<String, Object> analyzeKnowledgePoints(String userId) throws IOException {
        String cacheKey = "mistake:analysis:" + userId;
        String cacheValue = stringRedisTemplate.opsForValue().get(cacheKey);
        //存在缓存
        if (cacheValue != null) {
            return mapper.readValue(cacheValue, new TypeReference<Map<String, Object>>() {});
        }
        List<Mistake> mistakes=findByUserId(userId);
        //统计每个知识点出错次数
        Map<String, Integer> statistic = new HashMap<>();
        int cnt=0;
        for(Mistake mistake:mistakes){
            cnt++;
            String point=mistake.getKnowledgePoints();
            if (point == null||point.equals("无")) continue;
            point = point.replace("知识点:", "");
            String[] points = point.split(",");
            for (int i=0;i<points.length;i++){
                if(points[i].isEmpty() || points[i].equals("无")) continue;
                statistic.put(points[i],statistic.getOrDefault(points[i],0)+1);
            }
        }
        statistic.put("总错题数",cnt);
        String prompt = buildPrompt(statistic, cnt);
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("chatMode", "mistakeAnalysis");
        inputs.put("userId", userId);
        String result = difyUtils.callDify(inputs, prompt, userId);
        Map<String, Object> map = new HashMap<>();
        map.put("statistic", statistic);  //统计数据
        map.put("totalCount", cnt);
        map.put("aiAnalysis", result);    //AI分析文字
        stringRedisTemplate.opsForValue().set(cacheKey, mapper.writeValueAsString(map), 1, TimeUnit.HOURS);
        return map;
    }

    //旧版拼接prompt,用于错题分析
    @Override
    public String buildPrompt(Map<String, Integer> statistic, int totalCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("该用户共有").append(totalCount).append("道错题，知识点出错频率如下：\n");
        for (Map.Entry<String, Integer> entry : statistic.entrySet()) {
            if(entry.getKey().equals("总错题数")){continue;}
            sb.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("次\n");
        }
        sb.append("\n请根据以上数据:\n1. 总结该用户的主要薄弱点\n2. 给出具体的学习建议");
        return sb.toString();
    }

    //生成练习题
    @Override
    public List<Map<String, String>> generatePractice(String userId, List<String> weakPoints) throws IOException {
        StringBuilder prompt = new StringBuilder("用户的薄弱知识点如下，请针对这些知识点出5道练习题：\n");
        for (String weakPoint : weakPoints) {
            prompt.append("- "+weakPoint).append("\n");
        }
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("chatMode", "practiceGenerate");
        inputs.put("userId", userId);
        String result=difyUtils.callDify(inputs,prompt.toString(),userId);
        result = result.replaceAll("```json", "").replaceAll("```", "").trim();
        List<Map<String, String>> map = mapper.readValue(result, new TypeReference<List<Map<String, String>>>() {});
        return map;
    }

    //记忆曲线,用于计算下次复习时间
    @Override
    public LocalDate calcNextReviewTime(int reviewCount) {
        int[] intervals = {1, 2, 4, 7, 15};     //艾宾浩斯遗忘曲线
        int days = reviewCount < intervals.length ? intervals[reviewCount] : 30;      //超过五次复习,统一30天
        return LocalDate.now().plusDays(days);
    }

    //获取今日待复习
    @Override
    public List<Mistake> getTodayReview(String userId) {
        return mistakeMapper.selectList(
                Wrappers.<Mistake>lambdaQuery()
                        .eq(Mistake::getUserId, userId)
                        .le(Mistake::getNextReviewTime, LocalDate.now())        //到期或过期的
                        .isNotNull(Mistake::getNextReviewTime)                  //排除没有复习计划的
        );
    }
}
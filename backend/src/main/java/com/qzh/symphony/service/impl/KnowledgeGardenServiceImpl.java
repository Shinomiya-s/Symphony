package com.qzh.symphony.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qzh.symphony.DAO.KnowledgeGarden;
import com.qzh.symphony.DAO.KnowledgeTag;
import com.qzh.symphony.DAO.Mistake;
import com.qzh.symphony.DAO.TreeTag;
import com.qzh.symphony.common.utils.DifyUtils;
import com.qzh.symphony.common.utils.HanLPUtils;
import com.qzh.symphony.mapper.*;
import com.qzh.symphony.service.KnowledgeGardenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class KnowledgeGardenServiceImpl extends ServiceImpl<KnowledgeGardenMapper, KnowledgeGarden> implements KnowledgeGardenService {
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
    private final ObjectMapper mapper = new ObjectMapper();

    //生成知识园区详细单题
    @Override
    @Async("aiTaskExecutor")
    public void asyncGenerateKnowledgeGarden(Mistake mistake, String userId) {
        try {
            generateKnowledgeGarden(mistake, userId);
        } catch (Exception e) {
            System.err.println("知识园区生成失败: " + e.getMessage());
        }
    }

    @Override
    public void generateKnowledgeGarden(Mistake mistake, String userId) throws IOException {
        //第一层:计算hash，先做快速去重
        String hash = DigestUtils.md5DigestAsHex(
                mistake.getQuestion().getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );
        KnowledgeGarden existingByHash = knowledgeGardenMapper.selectOne(
                new LambdaQueryWrapper<KnowledgeGarden>()
                        .eq(KnowledgeGarden::getQuestionHash, hash)
        );
        if (existingByHash != null) return; //hash命中，直接跳过

        //第二层:语义比对-按tag缩小候选范围
        String[] newTags = mistake.getKnowledgePoints().split(",");
        LambdaQueryWrapper<KnowledgeGarden> tagQuery = new LambdaQueryWrapper<>();
        tagQuery.and(w -> {
            for (int i = 0; i < newTags.length; i++) {
                String t = newTags[i].trim();
                if (t.isEmpty() || t.equals("无")) continue;
                if (i == 0) w.like(KnowledgeGarden::getTags, t);
                else w.or().like(KnowledgeGarden::getTags, t);
            }
        });
        List<KnowledgeGarden> candidates = knowledgeGardenMapper.selectList(tagQuery);
        if (!candidates.isEmpty()) {
            //先用Jaccard快速过滤
            List<KnowledgeGarden> ambiguous = new ArrayList<>();
            for (KnowledgeGarden candidate : candidates) {
                double score = HanLPUtils.jaccardSimilarity(mistake.getQuestion(), candidate.getQuestion());
                if (score >= 0.7) return; //高度相似，直接判重复
                if (score > 0.2) ambiguous.add(candidate); //模糊区，留给AI
                //score<=0.2 直接排除，不进AI
            }

            //第三层:只有模糊区才走AI
            if (!ambiguous.isEmpty()) {
                StringBuilder semanticPrompt = new StringBuilder();
                semanticPrompt.append("新题目：").append(mistake.getQuestion()).append("\n\n");
                semanticPrompt.append("候选已有题目：\n");
                for (int i = 0; i < ambiguous.size(); i++) {
                    semanticPrompt.append(i + 1).append(". ").append(ambiguous.get(i).getQuestion()).append("\n");
                }
                semanticPrompt.append("\n以上候选题目中，是否有与新题目语义相同的题目？只回答 YES 或 NO。");
                Map<String, Object> dupInputs = new HashMap<>();
                dupInputs.put("chatMode", "duplicateCheck");
                dupInputs.put("userId", userId);
                String dupResult = difyUtils.callDify(dupInputs, semanticPrompt.toString(), userId);
                if (dupResult.trim().toUpperCase().contains("YES")) return;
            }
        }

        //通过去重后，才调Dify生成知识园区内容
        List<KnowledgeGarden> allKg = knowledgeGardenMapper.selectList(
                new LambdaQueryWrapper<KnowledgeGarden>()
                        .select(KnowledgeGarden::getKnowledgePoint)
                        .isNotNull(KnowledgeGarden::getKnowledgePoint)
        );

        String existingKps = knowledgeTagMapper.selectList(
                        new LambdaQueryWrapper<KnowledgeTag>()
                                .select(KnowledgeTag::getTagName)
                ).stream()
                .map(KnowledgeTag::getTagName)
                .distinct()
                .collect(Collectors.joining("、"));

        String prompt = "错题题目：" + mistake.getQuestion() + "\n" +
                "错误原因：" + mistake.getWrongReason() + "\n" +
                "正确答案：" + mistake.getCorrectAnswer() + "\n" +
                "知识点：" + mistake.getKnowledgePoints() + "\n\n" +
                "当前知识库已有知识点：" + (existingKps.isEmpty() ? "（暂无）" : existingKps) + "\n" +
                "请在 parent_knowledge_point 字段填入该知识点的直接上级知识点名称，必须从已有知识点中选择；如果已有知识点中没有合适的上级，则返回空字符串。";

        Map<String, Object> inputs = new HashMap<>();
        inputs.put("chatMode", "knowledgeGarden");
        inputs.put("userId", userId);
        String result = difyUtils.callDify(inputs, prompt, userId);
        result = result.replaceAll("```json", "").replaceAll("```", "").trim();
        Map<String, Object> resultMap = mapper.readValue(result, new TypeReference<Map<String, Object>>() {});

        //存库
        KnowledgeGarden kg = new KnowledgeGarden();
        kg.setKnowledgePoint((String) resultMap.get("knowledge_point"));
        kg.setQuestion((String) resultMap.get("question"));
        kg.setCorrectAnswer((String) resultMap.get("correct_answer"));
        kg.setAiAnalysis((String) resultMap.get("ai_analysis"));
        kg.setStrategy((String) resultMap.get("strategy"));
        kg.setTags((String) resultMap.get("tags"));
        kg.setWeight(1);
        kg.setFromUserId(userId);
        kg.setFromUsername(accountMapper.selectById(userId).getUsername());
        kg.setSourceMistakeId(mistake.getId());
        kg.setCreatedAt(LocalDateTime.now());
        kg.setQuestionHash(hash);

        //拿Dify返回的parentKnowledgePoint
        String parentKnowledgePoint = (String) resultMap.get("parent_knowledge_point");

        //先同步garden的tags进knowledge_tag
        if (kg.getTags() != null) {
            for (String raw : kg.getTags().split(",")) {
                String tagName = raw.trim();
                if (tagName.isEmpty()) continue;
                KnowledgeTag existing = knowledgeTagMapper.selectOne(
                        new LambdaQueryWrapper<KnowledgeTag>().eq(KnowledgeTag::getTagName, tagName));
                if (existing == null) {
                    KnowledgeTag newTag = new KnowledgeTag();
                    newTag.setTagName(tagName);
                    newTag.setQuestionCount(1);
                    newTag.setCreatedAt(LocalDateTime.now());
                    knowledgeTagMapper.insert(newTag);
                }
                //已有的不重复计数，count在extractKnowledgePoints里加过了
            }
        }
        knowledgeGardenMapper.insert(kg);
        syncTagsWithTree(kg.getTags(), parentKnowledgePoint);
    }

    private void syncTagsWithTree(String tags, String parentKnowledgePoint) {
        if (tags == null || tags.isBlank()) return;
        String[] tagArr = tags.split(",");

        for (String raw : tagArr) {
            String tag = raw.trim();
            if (tag.isEmpty()) continue;

            TreeTag existing = treeTagMapper.selectOne(
                    new LambdaQueryWrapper<TreeTag>().eq(TreeTag::getTagName, tag));
            if (existing != null) continue;

            KnowledgeTag kt = knowledgeTagMapper.selectOne(
                    new LambdaQueryWrapper<KnowledgeTag>().eq(KnowledgeTag::getTagName, tag));
            if (kt == null) continue;

            TreeTag newNode = new TreeTag();
            newNode.setId(kt.getId());
            newNode.setTagName(tag);

            //优先用Dify返回的parentKnowledgePoint找父节点
            TreeTag parent = null;
            if (parentKnowledgePoint != null && !parentKnowledgePoint.isBlank()) {
                parent = treeTagMapper.selectOne(
                        new LambdaQueryWrapper<TreeTag>().eq(TreeTag::getTagName, parentKnowledgePoint));
            }
            //(兜底) 没找到，从同批其他tag里找已存在的作为父,尽可能避免单独为根节点
            if (parent == null) {
                for (String other : tagArr) {
                    String otherTag = other.trim();
                    if (otherTag.isEmpty() || otherTag.equals(tag)) continue;
                    TreeTag candidate = treeTagMapper.selectOne(
                            new LambdaQueryWrapper<TreeTag>().eq(TreeTag::getTagName, otherTag));
                    if (candidate != null) {
                        parent = candidate;
                        break;
                    }
                }
            }

            if (parent != null) {
                //确定父节点后
                newNode.setParentId(parent.getId());
                //父的层级+1
                newNode.setLevel(parent.getLevel() + 1);
            } else {
                //没有父
                newNode.setParentId(null);
                //根节点
                newNode.setLevel(0);
            }

            treeTagMapper.insert(newNode);
        }
    }
}
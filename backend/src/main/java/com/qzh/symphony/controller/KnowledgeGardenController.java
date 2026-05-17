package com.qzh.symphony.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.qzh.symphony.DAO.KnowledgeGarden;
import com.qzh.symphony.DAO.KnowledgeLike;
import com.qzh.symphony.DAO.TreeTag;
import com.qzh.symphony.common.Result;
import com.qzh.symphony.mapper.KnowledgeGardenMapper;
import com.qzh.symphony.mapper.KnowledgeLikeMapper;
import com.qzh.symphony.mapper.TreeTagMapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/garden")
public class KnowledgeGardenController {
    @Autowired
    KnowledgeGardenMapper knowledgeGardenMapper;
    @Autowired
    TreeTagMapper treeTagMapper;
    @Autowired
    StringRedisTemplate redisTemplate;
    @Autowired
    KnowledgeLikeMapper knowledgeLikeMapper;
    @Autowired
    private RedissonClient redissonClient;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private static final String RELATED_KEY_PREFIX = "garden:related:";
    private static final long RELATED_TTL_HOURS = 12;

    //列表查询
    @GetMapping("/list")
    private Result listAllKnowledge(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String searchType,
            @RequestParam(required = false, defaultValue = "time") String sortBy
    ) {
        QueryWrapper<KnowledgeGarden> wrapper = new QueryWrapper<>();

        //搜索逻辑
        if (keyword != null && !keyword.trim().isEmpty()) {
            final String searchKey = keyword.trim();

            if ("title".equals(searchType)) {
                wrapper.like("question", searchKey);
            } else if ("tag".equals(searchType)) {
                wrapper.like("tags", searchKey);
            } else if ("user".equals(searchType)) {
                wrapper.like("from_username", searchKey);
            } else {
                wrapper.and(w -> w
                        .like("question", searchKey)
                        .or()
                        .like("tags", searchKey)
                        .or()
                        .like("from_username", searchKey)
                );
            }
        }
        if ("weight".equals(sortBy)) {
            //按热度倒序（热度高的在前）
            wrapper.orderByDesc("like_count");
        } else if ("tag".equals(sortBy)) {
            //按标签升序（A-Z）
            wrapper.orderByAsc("tags");
        } else {
            //默认按时间倒序（最新的在前）
            wrapper.orderByDesc("created_at");
        }

        List<KnowledgeGarden> list = knowledgeGardenMapper.selectList(wrapper);
        return Result.success(list);
    }

    //获取知识点
    @GetMapping("/{id}")
    private Result getKnowledgeById(@PathVariable String id){
        KnowledgeGarden kg = knowledgeGardenMapper.selectById(id);
        return Result.success(kg);
    }

    //获取相关推荐
    @GetMapping("/{id}/related")
    public Result getRelated(@PathVariable String id) {
        //Redis检查/存对应题目
        String cacheKey = RELATED_KEY_PREFIX + id;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                List list = objectMapper.readValue(cached, List.class);
                return Result.success(list);
            } catch (Exception ignored) {}
        }
        //当前详情题
        KnowledgeGarden current = knowledgeGardenMapper.selectById(id);
        if (current == null || current.getTags() == null) return Result.success(List.of());

        String[] currentTags = current.getTags().split(",");
        //父标签
        Set<String> parentTags = new LinkedHashSet<>();
        //子标签
        Set<String> childTags = new LinkedHashSet<>();
        //兄弟标签
        Set<String> siblingTags = new LinkedHashSet<>();

        //收集标签名,用于知道要找哪些标签
        for (String raw : currentTags) {
            String tag = raw.trim();
            if (tag.isEmpty()) continue;

            TreeTag node = treeTagMapper.selectOne(
                    new LambdaQueryWrapper<TreeTag>().eq(TreeTag::getTagName, tag));
            if (node == null) continue;

            //父
            if (node.getParentId() != null) {
                TreeTag parent = treeTagMapper.selectById(node.getParentId());
                if (parent != null) parentTags.add(parent.getTagName());

                //兄弟
                treeTagMapper.selectList(new LambdaQueryWrapper<TreeTag>()
                                .eq(TreeTag::getParentId, node.getParentId())
                                .ne(TreeTag::getId, node.getId()))
                        .forEach(s -> siblingTags.add(s.getTagName()));
            }

            //子
            treeTagMapper.selectList(new LambdaQueryWrapper<TreeTag>()
                            .eq(TreeTag::getParentId, node.getId()))
                    .forEach(c -> childTags.add(c.getTagName()));
        }

        Set<String> excludeIds = new HashSet<>();
        Set<String> excludePoints = new HashSet<>();
        excludeIds.add(id);
        if (current.getKnowledgePoint() != null) excludePoints.add(current.getKnowledgePoint());
        //用这些标签去找具体题目
        //最多四条,顺序:父->子->兄弟
        List<KnowledgeGarden> result = new ArrayList<>();
        int remaining;
        //父tag优先
        appendByTags(parentTags, excludeIds, excludePoints, result, 4);
        //剩余名额给子tag
        remaining = 4 - result.size();
        if (remaining > 0) appendByTags(childTags, excludeIds, excludePoints, result, remaining);
        //再剩余给兄弟tag
        remaining = 4 - result.size();
        if (remaining > 0) appendByTags(siblingTags, excludeIds, excludePoints, result, remaining);
        try {
            redisTemplate.opsForValue().set(cacheKey,
                    objectMapper.writeValueAsString(result),
                    RELATED_TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception ignored) {}
        return Result.success(result);
    }

    //Tag的排序
    private void appendByTags(Set<String> tags, Set<String> excludeIds,
                              Set<String> excludePoints,
                              List<KnowledgeGarden> result, int totalLimit) {
        if (tags.isEmpty() || result.size() >= totalLimit) return;
        int need = totalLimit - result.size();

        LambdaQueryWrapper<KnowledgeGarden> q = new LambdaQueryWrapper<>();
        if (!excludeIds.isEmpty()) q.notIn(KnowledgeGarden::getId, excludeIds);
        if (!excludePoints.isEmpty()) q.notIn(KnowledgeGarden::getKnowledgePoint, excludePoints);
        q.and(w -> {
            boolean first = true;
            for (String tag : tags) {
                if (first) { w.like(KnowledgeGarden::getTags, tag); first = false; }
                else w.or().like(KnowledgeGarden::getTags, tag);
            }
        });
        q.last("LIMIT " + need);

        List<KnowledgeGarden> found = knowledgeGardenMapper.selectList(q);
        found.forEach(k -> {
            excludeIds.add(k.getId());
            if (k.getKnowledgePoint() != null) excludePoints.add(k.getKnowledgePoint());
        });
        result.addAll(found);
    }

    //查询当前用户是否已点赞
    @GetMapping("/{id}/like")
    public Result getLikeStatus(@PathVariable String id) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        boolean liked = knowledgeLikeMapper.selectOne(
                new LambdaQueryWrapper<KnowledgeLike>()
                        .eq(KnowledgeLike::getKnowledgeId, id)
                        .eq(KnowledgeLike::getUserId, userId)) != null;
        return Result.success(liked);
    }

    //点赞/取消点赞
    @PostMapping("/{id}/like")
    public Result toggleLike(@PathVariable String id) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        //锁粒度到用户+知识点
        String lockKey = "lock:like:" + id + ":" + userId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            //最多等1秒拿锁，拿到后5秒内自动释放
            boolean acquired = lock.tryLock(1, 5, TimeUnit.SECONDS);
            if (!acquired) {
                return Result.error("操作太频繁，请稍后再试");
            }
            //查询是否点赞
            KnowledgeLike existing = knowledgeLikeMapper.selectOne(
                    new LambdaQueryWrapper<KnowledgeLike>()
                            .eq(KnowledgeLike::getKnowledgeId, id)
                            .eq(KnowledgeLike::getUserId, userId));
            if (existing != null) {
                //已点赞则取消
                knowledgeLikeMapper.deleteById(existing.getId());
                knowledgeGardenMapper.update(null, new LambdaUpdateWrapper<KnowledgeGarden>()
                        .eq(KnowledgeGarden::getId, id)
                        .setSql("like_count = GREATEST(like_count - 1, 0)"));
                return Result.success(false);
            } else {
                KnowledgeLike like = new KnowledgeLike();
                like.setKnowledgeId(id);
                like.setUserId(userId);
                like.setCreatedAt(LocalDateTime.now());
                //否则点赞
                knowledgeLikeMapper.insert(like);
                knowledgeGardenMapper.update(null, new LambdaUpdateWrapper<KnowledgeGarden>()
                        .eq(KnowledgeGarden::getId, id)
                        .setSql("like_count = like_count + 1"));
                return Result.success(true);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.error("系统繁忙");
        } finally {
            //释放锁且只有自己持有的锁才释放
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
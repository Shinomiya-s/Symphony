package com.qzh.symphony.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qzh.symphony.DAO.Account;
import com.qzh.symphony.DAO.AiMessage;
import com.qzh.symphony.DAO.Friendship;
import com.qzh.symphony.DAO.Mistake;
import com.qzh.symphony.common.utils.JwtUtils;
import com.qzh.symphony.mapper.AccountMapper;
import com.qzh.symphony.mapper.AiMessageMapper;
import com.qzh.symphony.mapper.FriendshipMapper;
import com.qzh.symphony.mapper.MistakeMapper;
import com.qzh.symphony.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class AccountServiceImpl extends ServiceImpl<AccountMapper, Account> implements AccountService {
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private AccountMapper accountMapper;
    @Autowired
    private FriendshipMapper friendshipMapper;
    @Autowired
    private MistakeMapper mistakeMapper;
    @Autowired
    private AiMessageMapper aiMessageMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    //登录
    @Override
    public Map<String, Object> login(String phone, String password) {
        //手机号格式验证
        if (!phone.matches("^\\d{11}$")) {
            throw new RuntimeException("手机号格式不正确");
        }

        //Redis防爆:同一手机号60秒内超过5次锁定15分钟
        String limitKey = "limit:login:" + phone;
        String lockKey = "lock:login:" + phone;
        if (stringRedisTemplate.hasKey(lockKey)) {
            throw new RuntimeException("登录失败次数过多，请15分钟后再试");
        }
        Account account = baseMapper.selectOne(new LambdaQueryWrapper<Account>()
                .eq(Account::getPhone, phone));
        if (account == null) {
            throw new RuntimeException("该用户尚未注册");
        }
        //MD5 校验密码
        String md5Password = DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));
        if (!md5Password.equals(account.getPassword())) {
            //失败次数+1
            Long count = stringRedisTemplate.opsForValue().increment(limitKey);
            if (count == 1) stringRedisTemplate.expire(limitKey, 60, TimeUnit.SECONDS);
            if (count >= 5) {
                stringRedisTemplate.opsForValue().set(lockKey, "1", 15, TimeUnit.MINUTES);
                stringRedisTemplate.delete(limitKey);
                throw new RuntimeException("登录失败次数过多，请15分钟后再试");
            }
            throw new RuntimeException("密码不正确，请重新输入");
        }
        //登录成功，清除失败计数
        stringRedisTemplate.delete(limitKey);
        account.setLastLoginTime(LocalDateTime.now());
        updateById(account);
        //登录成功后返回token，并把密码置空再返回给前端：
        String token = jwtUtils.createToken(account.getUserId(), account.getUsername());
        //组装数据
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        //敏感信息脱敏
        account.setPassword(null);
        result.put("user", account);
        return result;
    }

    //注册
    @Override
    public boolean register(Account account) {
        //手机号格式验证
        if (!account.getPhone().matches("^\\d{11}$")) {
            throw new RuntimeException("手机号格式不正确");
        }
        //Redis 防爆:同一手机号60秒内超过3次注册
        String limitKey = "limit:register:" + account.getPhone();
        Long count = stringRedisTemplate.opsForValue().increment(limitKey);
        if (count == 1) stringRedisTemplate.expire(limitKey, 60, TimeUnit.SECONDS);
        if (count > 3) {
            throw new RuntimeException("注册太频繁，请稍后再试");
        }
        //检查手机号是否已存在
        Long phoneCount = baseMapper.selectCount(new LambdaQueryWrapper<Account>()
                .eq(Account::getPhone, account.getPhone()));
        if (phoneCount > 0) {
            throw new RuntimeException("该手机号已被注册");
        }
        //检查用户名是否已存在
        Long usernameCount = baseMapper.selectCount(new LambdaQueryWrapper<Account>()
                .eq(Account::getUsername, account.getUsername()));
        if (usernameCount > 0) {
            throw new RuntimeException("该用户名已被占用");
        }
        //密码加密
        String encodedPassword = DigestUtils.md5DigestAsHex(account.getPassword().getBytes(StandardCharsets.UTF_8));
        account.setPassword(encodedPassword);
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());
        //执行插入
        return baseMapper.insert(account) > 0;
    }

    //查看自己主页
    @Override
    public Account getAccountProfile(String userId) {
        return baseMapper.selectById(userId);
    }

    //统计好友数
    @Override
    public Long getFriendCount(String userId) {
        return friendshipMapper.selectCount(
                new LambdaQueryWrapper<Friendship>()
                        .and(w -> w
                                .eq(Friendship::getUserId, userId)
                                .or()
                                .eq(Friendship::getFriendId, userId)
                        )
        );
    }

    //更新用户偏好设置
    @Override
    public boolean updatePreferences(String userId, Account preferences) {
        //设置要更新的用户
        preferences.setUserId(userId);
        preferences.setUpdatedAt(LocalDateTime.now());
        //调用查询方法
        int result = baseMapper.updateById(preferences);
        return result > 0;
    }

    //查看他人主页
    @Override
    public Map<String, Object> getUserProfile(String username, String currentUserId) {
        Account user=accountMapper.selectOne(new LambdaQueryWrapper<Account>()
                .eq(Account::getUsername, username));
        Map<String,Object> map=new HashMap<>();
        map.put("username",user.getUsername());
        map.put("nickname",user.getNickname());
        map.put("bio",user.getBio());
        map.put("mistakeCount",user.getMistakeCount());
        map.put("aiCount",user.getAiCount());
        long days = ChronoUnit.DAYS.between(user.getCreatedAt().toLocalDate(), LocalDate.now()) + 1;
        map.put("days", days);
        String targetUserId = user.getUserId();
        String smallId = currentUserId.compareTo(targetUserId) < 0 ? currentUserId : targetUserId;
        String bigId = currentUserId.compareTo(targetUserId) < 0 ? targetUserId : currentUserId;
        boolean isFriend = friendshipMapper.selectOne(
                new LambdaQueryWrapper<Friendship>()
                        .eq(Friendship::getUserId, smallId)
                        .eq(Friendship::getFriendId, bigId)
        ) != null;
        map.put("isFriend", isFriend);
        return map;
    }

    //统计错题数
    @Override
    public Integer getMistakeCount(String userId) {
        LambdaQueryWrapper<Mistake> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Mistake::getUserId, userId);
        return Math.toIntExact(mistakeMapper.selectCount(wrapper));
    }

    //统计对话数
    @Override
    public Integer getTotalConversations(String userId) {
        LambdaQueryWrapper<AiMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiMessage::getUserId, userId)
                .eq(AiMessage::getRole, "user");  // 只统计用户发送的消息
        return Math.toIntExact(aiMessageMapper.selectCount(wrapper));
    }

    //计算掌握度
    public Double getKnowledgeMastery(String userId) {
        //总错题数
        LambdaQueryWrapper<Mistake> totalWrapper = new LambdaQueryWrapper<>();
        totalWrapper.eq(Mistake::getUserId, userId);
        long totalCount = mistakeMapper.selectCount(totalWrapper);

        if (totalCount == 0) {
            return 0.0;  //没有错题时掌握度为0%
        }

        //已掌握的错题数
        LambdaQueryWrapper<Mistake> masteredWrapper = new LambdaQueryWrapper<>();
        masteredWrapper.eq(Mistake::getUserId, userId)
                .eq(Mistake::getStatus, "已掌握");
        long masteredCount = mistakeMapper.selectCount(masteredWrapper);

        //计算百分比
        return (masteredCount * 100.0) / totalCount;
    }
}

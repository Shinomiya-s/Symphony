package com.qzh.symphony.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qzh.symphony.DAO.Account;
import com.qzh.symphony.DAO.DailyReport;
import com.qzh.symphony.DAO.PrivateMessage;
import com.qzh.symphony.common.utils.DifyUtils;
import com.qzh.symphony.mapper.AccountMapper;
import com.qzh.symphony.mapper.DailyReportMapper;
import com.qzh.symphony.mapper.PrivateMessageMapper;
import com.qzh.symphony.service.impl.MistakeAiServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class DailyReportTask {
    @Autowired
    private PrivateMessageMapper privateMessageMapper;
    @Autowired
    private DailyReportMapper dailyReportMapper;
    @Autowired
    private AccountMapper accountMapper;
    @Autowired
    private MistakeAiServiceImpl mistakeAiService;
    @Autowired
    private DifyUtils difyUtils;

    //凌晨一点运行
    @Scheduled(cron = "0 0 1 * * ?")
    public void generateDailyReports() {
        LocalDateTime startOfYesterday = LocalDate.now().minusDays(1).atStartOfDay();
        LocalDateTime endOfYesterday = LocalDate.now().atStartOfDay();

        List<PrivateMessage> messages = privateMessageMapper.selectList(
                new LambdaQueryWrapper<PrivateMessage>()        //查昨天所有私聊消息（type=0排除AI消息）
                        .ge(PrivateMessage::getCreatedAt, startOfYesterday)
                        .lt(PrivateMessage::getCreatedAt, endOfYesterday)
                        .eq(PrivateMessage::getType, 0)
        );
        if (messages.isEmpty()) return;
        //用seen Set去重，保证每对用户只处理一次
        Set<String> seen = new HashSet<>();
        for (PrivateMessage msg : messages) {
            String[] pair = normalizePair(msg.getFromUserId(), msg.getToUserId());
            if (seen.add(pair[0] + "_" + pair[1])) {
                try {
                    generateReportForPair(pair[0], pair[1], messages);
                } catch (Exception e) {
                    System.err.println("日报生成失败，跳过该对用户: " + pair[0] + "_" + pair[1] + "，原因: " + e.getMessage());
                }
            }
        }
    }

    //生成一对用户的日报
    private void generateReportForPair(String u1, String u2, List<PrivateMessage> allMessages) throws IOException {
        //已生成过就跳过
        Long count = dailyReportMapper.selectCount(
                new LambdaQueryWrapper<DailyReport>()
                        .eq(DailyReport::getUserId, u1)
                        .eq(DailyReport::getTargetUserId, u2)
                        .eq(DailyReport::getReportDate, LocalDate.now().minusDays(1))
                        .eq(DailyReport::getType, 0)
        );
        if (count > 0) return;

        List<PrivateMessage> chatMessages = allMessages.stream()
                .filter(m -> (m.getFromUserId().equals(u1) && m.getToUserId().equals(u2))
                        || (m.getFromUserId().equals(u2) && m.getToUserId().equals(u1)))
                .sorted(Comparator.comparing(PrivateMessage::getCreatedAt)) // 加这行
                .collect(Collectors.toList());
        if (chatMessages.isEmpty()) return;

        Account a1 = accountMapper.selectOne(new LambdaQueryWrapper<Account>().eq(Account::getUserId, u1));
        Account a2 = accountMapper.selectOne(new LambdaQueryWrapper<Account>().eq(Account::getUserId, u2));
        String name1 = a1 != null ? a1.getUsername() : u1;
        String name2 = a2 != null ? a2.getUsername() : u2;

        //拼对话文本
        StringBuilder chatText = new StringBuilder();
        for (PrivateMessage msg : chatMessages) {
            chatText.append(msg.getFromUserId().equals(u1) ? name1 : name2)
                    .append("：").append(msg.getContent()).append("\n");
        }
        //调Dify生成摘要，插两条记录
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("chatMode", "dailyReport");
        String summary = difyUtils.callDify(inputs, chatText.toString(), u1 + "_" + u2);

        DailyReport report = new DailyReport();
        report.setUserId(u1);
        report.setTargetUserId(u2);
        report.setReportDate(LocalDate.now().minusDays(1));
        report.setContent(summary);
        report.setType(0);
        report.setIsRead(0);
        report.setCreatedAt(LocalDateTime.now());
        dailyReportMapper.insert(report);

        DailyReport report2 = new DailyReport();
        report2.setUserId(u2);
        report2.setTargetUserId(u1);
        report2.setReportDate(LocalDate.now().minusDays(1));
        report2.setContent(summary);
        report2.setType(0);
        report2.setIsRead(0);
        report2.setCreatedAt(LocalDateTime.now());
        dailyReportMapper.insert(report2);
    }

    //重复代码,与ChatUtils里的一致
    private String[] normalizePair(String a, String b) {
        return a.compareTo(b) <= 0 ? new String[]{a, b} : new String[]{b, a};
    }
}
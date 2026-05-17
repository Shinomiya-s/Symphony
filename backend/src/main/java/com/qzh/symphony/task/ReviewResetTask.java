package com.qzh.symphony.task;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qzh.symphony.DAO.Mistake;
import com.qzh.symphony.mapper.MistakeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

//遗忘曲线任务
@Component
public class ReviewResetTask {

    @Autowired
    private MistakeMapper mistakeMapper;
    //每天00:00执行
    @Scheduled(cron = "0 0 0 * * ?")
    public void resetReviewStatus() {
        //调自定义SQL
        int affectedRows = mistakeMapper.resetToReview();
        System.out.println("遗忘曲线定时任务执行完成，更新了 " + affectedRows + " 道错题为待复习状态");
    }
}
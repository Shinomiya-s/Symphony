package com.qzh.symphony.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qzh.symphony.DAO.Mistake;
import org.apache.ibatis.annotations.Update;

public interface MistakeMapper extends BaseMapper<Mistake> {
    //用普通update语句来修改状态
    @Update("UPDATE mistake SET status='待复习', status_updated_at=NOW() " +
            "WHERE next_review_time <= CURDATE() " +
            "AND (status != '已掌握' OR (status = '已掌握' AND (status_updated_at < CURDATE() OR status_updated_at IS NULL)))")
    int resetToReview();
}

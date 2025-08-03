package com.miku.lottery.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page; // 导入 Page 类
import com.miku.lottery.entity.LotteryRecord;
import com.miku.lottery.mapper.LotteryRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 抽奖记录业务服务类
 */
@Service
public class LotteryRecordService {

    @Autowired
    private LotteryRecordMapper lotteryRecordMapper; // 注入 LotteryRecordMapper

    /**
     * 查询指定用户的抽奖历史记录，支持分页。
     *
     * @param userId 用户ID
     * @param pageNum 页码 (从1开始)
     * @param pageSize 每页大小
     * @return 分页的抽奖记录列表
     */
    public Page<LotteryRecord> getRecordsByUserId(Long userId, int pageNum, int pageSize) {
        Page<LotteryRecord> page = new Page<>(pageNum, pageSize); // 创建分页对象
        QueryWrapper<LotteryRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId) // 查询指定用户ID的记录
                .orderByDesc("draw_time"); // 按抽奖时间倒序排列（最新记录在前）
        return lotteryRecordMapper.selectPage(page, queryWrapper);
    }
}

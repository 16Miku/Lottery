package com.miku.lottery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miku.lottery.entity.LotteryRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 抽奖记录表的 Mapper 接口
 */
@Mapper // 声明为 MyBatis 的 Mapper 接口
public interface LotteryRecordMapper extends BaseMapper<LotteryRecord> {
    // BaseMapper 提供了 LotteryRecord 表的常用 CRUD 方法，无需额外编写
}

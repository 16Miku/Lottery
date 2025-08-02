package com.miku.lottery.mapper;



import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.miku.lottery.entity.Prize;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * Prize 表的 Mapper 接口
 */
@Mapper // 声明为 MyBatis 的 Mapper 接口
public interface PrizeMapper extends BaseMapper<Prize> {

    /**
     * 扣减库存（使用乐观锁）
     * @param id 奖品ID
     * @param version 版本号
     * @return 更新的行数，0表示失败，1表示成功
     */
    @Update("UPDATE prize SET remaining_quantity = remaining_quantity - 1, version = version + 1 " +
            "WHERE id = #{id} AND remaining_quantity > 0 AND version = #{version}")
    int deductStock(@Param("id") Long id, @Param("version") Integer version);
}

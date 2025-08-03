package com.miku.lottery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miku.lottery.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表的 Mapper 接口
 */
@Mapper // 声明为 MyBatis 的 Mapper 接口
public interface UserMapper extends BaseMapper<User> {
    // BaseMapper 提供了 User 表的常用 CRUD 方法，无需额外编写
}

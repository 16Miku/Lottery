package com.miku.lottery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable; // 导入 Serializable 接口
import java.time.LocalDateTime;

/**
 * 抽奖记录实体类，映射数据库 lottery_record 表
 */
@Data // Lombok 注解，自动生成 getter, setter, toString 等方法
@TableName("lottery_record") // 映射到数据库的 "lottery_record" 表
public class LotteryRecord implements Serializable { // 实现 Serializable 接口，以备将来缓存需求

    private static final long serialVersionUID = 1L; // 序列化版本号，推荐添加

    @TableId(type = IdType.AUTO) // 声明主键并设置为数据库自增
    private Long id; // 记录唯一标识符

    private Long userId; // 用户ID，关联 user 表

    private Long prizeId; // 奖品ID，关联 prize 表 (如果中奖)

    private String prizeName; // 奖品名称 (冗余存储)

    private Integer isWinning; // 是否中奖 (0: 未中奖, 1: 中奖)

    private LocalDateTime drawTime; // 抽奖时间
}

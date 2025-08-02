package com.miku.lottery.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 奖品实体类
 */
@Data // Lombok 注解，自动生成 getter, setter, toString 等方法
@TableName("prize") // 映射到数据库的 prize 表
public class Prize {

    @TableId(type = IdType.AUTO) // 声明主键并设置为自增
    private Long id;

    // 奖品名称
    private String prizeName;

    // 总库存
    private Integer totalQuantity;

    // 剩余库存
    private Integer remainingQuantity;

    // 中奖概率
    private BigDecimal probability;

    @Version // 声明为乐观锁版本号字段
    private Integer version;

    // 创建时间
    private LocalDateTime createTime;

    // 更新时间
    private LocalDateTime updateTime;
}

package com.miku.lottery.service;

// 1. 删掉 import javax.annotation.Resource;
// 2. 引入 Autowired
import org.springframework.beans.factory.annotation.Autowired;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.miku.lottery.entity.Prize;
import com.miku.lottery.mapper.PrizeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

@Service
public class LotteryService {

    @Autowired // 3. 将 @Resource 修改为 @Autowired
    private PrizeMapper prizeMapper;

    // ... 其他代码不变 ...
    private final Random random = new Random();

    /**
     * 执行抽奖
     * @return 返回中奖的奖品信息，如果未中奖则返回 null
     */
    @Transactional(rollbackFor = Exception.class) // 开启事务
    public Prize draw() {
        // 1. 获取所有库存大于0的奖品列表
        List<Prize> availablePrizes = prizeMapper.selectList(
                new QueryWrapper<Prize>().gt("remaining_quantity", 0)
        );

        // 如果没有可用奖品，直接返回 null
        if (availablePrizes.isEmpty()) {
            return null;
        }

        // 2. 计算总概率（可以小于1）
        BigDecimal totalProbability = availablePrizes.stream()
                .map(Prize::getProbability)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. 生成一个 0 到 totalProbability 之间的随机数
        BigDecimal randomValue = totalProbability.multiply(new BigDecimal(random.nextDouble()));

        // 4. 轮盘赌算法：确定中奖奖品
        Prize winningPrize = null;
        BigDecimal currentSum = BigDecimal.ZERO;
        for (Prize prize : availablePrizes) {
            currentSum = currentSum.add(prize.getProbability());
            if (randomValue.compareTo(currentSum) < 0) {
                winningPrize = prize;
                break;
            }
        }

        // 5. 如果根据概率没有抽中任何奖品（例如总概率小于1），则返回 "谢谢参与"
        if (winningPrize == null) {
            return null;
        }

        // 6. 核心步骤：使用乐观锁扣减库存
        int affectedRows = prizeMapper.deductStock(winningPrize.getId(), winningPrize.getVersion());

        // 7. 如果扣减失败（通常是高并发导致），说明其他线程已修改，本次抽奖算作未中奖
        if (affectedRows == 0) {
            // 可以选择重试，或直接返回未中奖
            System.out.println("库存扣减失败，奖品ID: " + winningPrize.getId() + "，版本号: " + winningPrize.getVersion());
            return null;
        }

        // 8. 扣减成功，返回中奖奖品信息
        return winningPrize;
    }
}

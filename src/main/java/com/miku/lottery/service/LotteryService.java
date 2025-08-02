package com.miku.lottery.service;

// 1. 删掉 import javax.annotation.Resource;
// 2. 引入 Autowired
import org.springframework.beans.factory.annotation.Autowired;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.miku.lottery.entity.Prize;
import com.miku.lottery.mapper.PrizeMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

@Service
public class LotteryService {

    @Autowired // 3. 将 @Resource 修改为 @Autowired
    private PrizeMapper prizeMapper;



    // 关键改动：自注入 LotteryService 实例的代理对象
    // 这样在 draw() 方法内部调用 getAllAvailablePrizes() 时，
    // 可以通过这个代理对象调用，从而使 @Cacheable 生效。
    @Autowired
    private LotteryService self; // 注入自身代理
    /*问题分析：Spring AOP 自调用失效问题 (Self-Invocation Issue)
    这是一个在使用 Spring AOP (Spring Cache 是基于 AOP 实现的) 时非常常见且容易踩的坑，被称为“自调用失效”问题。

    原理是这样的：
    1、Spring AOP 通过代理来为 Bean 添加横切逻辑（例如缓存、事务）。当你使用 @Cacheable 或 @Transactional 注解时，Spring 会为你的 LotteryService 创建一个代理对象。
    2、当 LotteryController 调用 lotteryService.draw() 时，它实际上调用的是 lotteryService 的代理对象。这个代理对象会拦截 draw() 方法的调用，并根据 @Transactional 和 @CacheEvict 的规则进行处理。
    3、问题出在 draw() 方法内部调用 getAllAvailablePrizes() 时。此时，draw() 方法是通过 this.getAllAvailablePrizes() 来调用的，这里的 this 指向的是 LotteryService 的原始对象（Target Object），而不是 Spring 创建的代理对象。
    4、由于调用的是原始对象的方法，Spring 的 AOP 代理机制无法拦截到这个内部方法调用，因此 @Cacheable 注解就不会生效。

    解决方案：通过自注入（Self-Injection）解决 AOP 自调用问题
    解决这个问题最常见且简洁的方法是让 LotteryService 通过 @Autowired 将它自己的代理实例注入给自己。这样，当 draw() 方法需要调用 getAllAvailablePrizes() 时，它就可以通过注入的代理实例来调用，从而确保 AOP 能够拦截到并应用缓存逻辑。
    */



    private final Random random = new Random();




    /**
     * 获取所有库存大于0的奖品列表。
     * 该方法的查询结果会被缓存到名为 "prizes" 的缓存空间中。
     * 缓存键固定为 "availablePrizesList"。
     *
     * @return 可用的奖品列表
     */
    @Cacheable(value = "prizes", key = "'availablePrizesList'")
    // value = "prizes": 指定缓存的名称，Redis 中会以这个名称作为 key 的前缀。
    // key = "'availablePrizesList'": 指定缓存的键。这里使用一个固定的字符串作为键，
    //                                因为我们总是缓存所有可用奖品的列表。
    //                                注意：字符串需要用单引号或双引号包裹。
    public List<Prize> getAllAvailablePrizes() {
        System.out.println("从数据库加载可用奖品列表..."); // 打印日志，观察是否命中缓存
        return prizeMapper.selectList(
                new QueryWrapper<Prize>().gt("remaining_quantity", 0)
        );
    }




    /**
     * 执行抽奖
     *
     * @return 返回中奖的奖品信息，如果未中奖则返回 null
     */
    @Transactional(rollbackFor = Exception.class)   // 开启事务
    public Prize draw() {

        // 1. 获取所有库存大于0的奖品列表 (现在通过 self.getAllAvailablePrizes() 调用，确保缓存生效)
        // 注意这里从 this.getAllAvailablePrizes() 改为 self.getAllAvailablePrizes()
        List<Prize> availablePrizes = self.getAllAvailablePrizes();

        // 如果没有可用奖品，直接返回 null
        if (availablePrizes.isEmpty()) {

            System.out.println("当前没有可用奖品。");
            return null;
        }

        // 2. 计算总概率（可以小于1）
        BigDecimal totalProbability = availablePrizes.stream()
                .map(Prize::getProbability)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 如果总概率为0，也无法抽奖
        if (totalProbability.compareTo(BigDecimal.ZERO) == 0) {
            System.out.println("所有可用奖品的总概率为0，无法抽奖。");
            return null;
        }

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

            System.out.println("随机数未落在任何奖品区间，未中奖。");
            return null;
        }

        // 6. 核心步骤：使用乐观锁扣减库存
        // 注意：这里扣减库存是直接操作数据库，不会影响缓存
        int affectedRows = prizeMapper.deductStock(winningPrize.getId(), winningPrize.getVersion());

        // 7. 如果扣减失败（通常是高并发导致），说明其他线程已修改，本次抽奖算作未中奖
        if (affectedRows == 0) {

            System.out.println("库存扣减失败，奖品ID: " + winningPrize.getId() + "，版本号: " + winningPrize.getVersion() + "。可能是并发冲突导致。");
            return null;
        }

        // 8. 扣减成功，返回中奖奖品信息
        System.out.println("恭喜抽中奖品: " + winningPrize.getPrizeName());
        return winningPrize;
    }
}

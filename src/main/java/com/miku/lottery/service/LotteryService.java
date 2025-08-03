package com.miku.lottery.service;

// 1. 删掉 import javax.annotation.Resource;
// 2. 引入 Autowired
import com.miku.lottery.entity.LotteryRecord;
import com.miku.lottery.entity.User;
import com.miku.lottery.mapper.LotteryRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.miku.lottery.entity.Prize;
import com.miku.lottery.mapper.PrizeMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class LotteryService {

    @Autowired // 3. 将 @Resource 修改为 @Autowired
    private PrizeMapper prizeMapper;

    @Autowired
    private LotteryRecordMapper lotteryRecordMapper;
    // 注入 LotteryRecordMapper

    // 关键改动：自注入 LotteryService 实例的代理对象
    // 这样在 draw() 方法内部调用 getAllAvailablePrizes() 时，
    // 可以通过这个代理对象调用，从而使 @Cacheable 生效。
    @Autowired
    private LotteryService self;
    // 注入自身代理
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


    @Autowired
    private UserService userService;
    // 注入 UserService，用于获取用户详情

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    // 注入 RedisTemplate




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
        System.out.println("从数据库加载可用奖品列表...");
        // 打印日志，观察是否命中缓存
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


        // 获取当前登录用户的用户名
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        // 根据用户名获取用户ID
        User currentUser = userService.findByUsername(username);
        if (currentUser == null) {
            // 理论上不会发生，因为请求已经通过 JWT 过滤器认证
            System.err.println("错误：认证用户 " + username + " 未找到！");
            return null;
        }
        Long userId = currentUser.getId();
        // 获取用户ID



        // -------------------- 抽奖次数限制逻辑 START --------------------
        // 构建 Redis Key：user_daily_draw_count:用户ID:YYYY-MM-DD
        String dailyDrawCountKey = "user_daily_draw_count:" + userId + ":" + LocalDate.now();
        // 设置每日最大抽奖次数
        int maxDailyDraws = 3;
        // 示例：每人每天最多抽奖 3 次

        // 使用 Redis 的 INCR 命令来原子性地增加抽奖次数并获取当前次数
        // increment 返回的是增加后的新值
        Long currentDrawCount = redisTemplate.opsForValue().increment(dailyDrawCountKey);

        // 如果是第一次抽奖（currentDrawCount 为 1），设置 Key 的过期时间为当天结束
        if (currentDrawCount != null && currentDrawCount == 1) {
            // 计算到当天午夜 23:59:59 的剩余秒数，作为过期时间
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime midnight = LocalDate.now().plusDays(1).atStartOfDay();
            long secondsUntilMidnight = Duration.between(now, midnight).getSeconds();
            redisTemplate.expire(dailyDrawCountKey, secondsUntilMidnight, TimeUnit.SECONDS);
            System.out.println("用户 " + username + " 今日首次抽奖，设置抽奖次数缓存过期时间为 " + secondsUntilMidnight + " 秒。");
        }

        // 检查是否超过每日抽奖限制
        if (currentDrawCount != null && currentDrawCount > maxDailyDraws) {
            System.out.println("用户 " + username + " 今日抽奖次数已达上限 (" + maxDailyDraws + " 次)。");
            // 返回一个特定的业务错误码或消息，表示抽奖次数已用完
            // 为了与现有接口返回类型兼容，这里返回 null，前端会显示“未中奖”
            // 实际项目中可以定义更细致的返回码和消息
            return null;
        }
        System.out.println("用户 " + username + " 今日已抽奖 " + currentDrawCount + " 次。");
        // -------------------- 抽奖次数限制逻辑 END --------------------




        // 1. 获取所有库存大于0的奖品列表 (现在通过 self.getAllAvailablePrizes() 调用，确保缓存生效)
        // 注意这里从 this.getAllAvailablePrizes() 改为 self.getAllAvailablePrizes()
        List<Prize> availablePrizes = self.getAllAvailablePrizes();

        // 如果没有可用奖品，直接返回 null
        if (availablePrizes.isEmpty()) {

            // 如果没有可用奖品，这次抽奖也不应该算作有效次数，所以需要回滚 Redis 的 INCR 操作
            // 但由于 Redis 是原子操作且无法回滚，通常的做法是：
            // 1. 在抽奖前先检查是否有可用奖品，如果没有直接返回，不增加次数。
            // 2. 或者，如果 INCR 已经执行，可以在这里再 DECR 一次。但 DECR 存在并发问题。
            // 3. 最健壮的做法是，将抽奖次数的判断和增加放在事务的**外部**，或者使用 Lua 脚本保证原子性。
            //    但为了简化，我们暂时接受这种“即使没奖也可能增加次数”的情况，因为最终会过期。

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


        // 在执行扣减之前，先准备好抽奖记录对象，以便无论成功失败都能记录
        LotteryRecord record = new LotteryRecord();
        record.setUserId(userId);
        record.setDrawTime(LocalDateTime.now());
        // 设置抽奖时间



        // 5. 如果根据概率没有抽中任何奖品（例如总概率小于1），则返回 "谢谢参与"
        if (winningPrize == null) {

            System.out.println("随机数未落在任何奖品区间，未中奖。");

            record.setIsWinning(0);
            // 设置为未中奖
            record.setPrizeName("谢谢参与");
            // 明确记录未中奖的名称
            lotteryRecordMapper.insert(record);
            // 插入抽奖记录

            return null;
        }

        // 6. 核心步骤：使用乐观锁扣减库存
        // 注意：这里扣减库存是直接操作数据库，不会影响缓存
        int affectedRows = prizeMapper.deductStock(winningPrize.getId(), winningPrize.getVersion());

        // 7. 如果扣减失败（通常是高并发导致），说明其他线程已修改，本次抽奖算作未中奖
        if (affectedRows == 0) {

            System.out.println("库存扣减失败，奖品ID: " + winningPrize.getId() + "，版本号: " + winningPrize.getVersion() + "。可能是并发冲突导致。");

            record.setIsWinning(0);
            // 设置为未中奖（虽然抽中了，但没扣到库存，实际未获得）
            record.setPrizeId(winningPrize.getId());
            record.setPrizeName(winningPrize.getPrizeName() + "(库存不足/并发冲突)");
            // 记录具体原因
            lotteryRecordMapper.insert(record);
            // 插入抽奖记录

            return null;
        }

        // 扣减成功，记录中奖信息
        System.out.println("恭喜用户 " + username + " 抽中奖品: " + winningPrize.getPrizeName());

        record.setIsWinning(1);
        // 设置为中奖
        record.setPrizeId(winningPrize.getId());
        record.setPrizeName(winningPrize.getPrizeName());
        lotteryRecordMapper.insert(record);
        // 插入抽奖记录

        return winningPrize;
    }
}

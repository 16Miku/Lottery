package com.miku.lottery.controller;

// 1. 删掉 import javax.annotation.Resource;
// 2. 引入 Autowired
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.miku.lottery.entity.LotteryRecord;
import com.miku.lottery.entity.User;
import com.miku.lottery.service.LotteryRecordService;
import com.miku.lottery.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import com.miku.lottery.entity.Prize;
import com.miku.lottery.service.LotteryService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/lottery")
public class LotteryController {

    @Autowired // 3. 将 @Resource 修改为 @Autowired
    private LotteryService lotteryService;


    @Autowired
    private LotteryRecordService lotteryRecordService; // 注入 LotteryRecordService

    @Autowired
    private UserService userService; // 注入 UserService



    @Autowired
    private RedisTemplate<String, Object> redisTemplate; // 注入 RedisTemplate




    // ... (drawLottery 方法不变) ...
    @PostMapping("/draw")
    public Map<String, Object> drawLottery() {
        Map<String, Object> result = new HashMap<>();

        // 调用 Service 执行抽奖
        Prize prize = lotteryService.draw();

        if (prize != null) {
            // 中奖了
            result.put("code", 200);
            result.put("message", "恭喜你，中奖了！");
            result.put("data", prize.getPrizeName());
        } else {
            // 未中奖
            result.put("code", 404);
            result.put("message", "很遗憾，未中奖，谢谢参与！");
            result.put("data", null);
        }

        return result;
    }




    /**
     * 查询当前用户的抽奖历史记录接口
     *
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 抽奖记录分页列表
     */
    @GetMapping("/records")
    public ResponseEntity<Map<String, Object>> getLotteryRecords(
            @RequestParam(defaultValue = "1") int pageNum, // 默认页码为1
            @RequestParam(defaultValue = "10") int pageSize // 默认每页大小为10
    ) {
        Map<String, Object> response = new HashMap<>();
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userService.findByUsername(username);

        if (currentUser == null) {
            response.put("code", 401);
            response.put("message", "用户未登录或会话失效。");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }




        Page<LotteryRecord> recordsPage = lotteryRecordService.getRecordsByUserId(currentUser.getId(), pageNum, pageSize);

        response.put("code", 200);
        response.put("message", "查询成功。");
        response.put("data", recordsPage.getRecords()); // 返回当前页的记录列表
        response.put("total", recordsPage.getTotal()); // 返回总记录数
        response.put("pages", recordsPage.getPages()); // 返回总页数
        response.put("current", recordsPage.getCurrent()); // 返回当前页码
        response.put("size", recordsPage.getSize()); // 返回每页大小

        return new ResponseEntity<>(response, HttpStatus.OK);
    }




    /**
     * 获取当前用户今日剩余抽奖次数接口
     * @return 包含剩余抽奖次数的响应
     */
    @GetMapping("/user/draw-count")
    public ResponseEntity<Map<String, Object>> getUserDrawCount() {
        Map<String, Object> response = new HashMap<>();
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userService.findByUsername(username);

        if (currentUser == null) {
            response.put("code", 401);
            response.put("message", "用户未登录或会话失效。");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }
        Long userId = currentUser.getId();

        String dailyDrawCountKey = "user_daily_draw_count:" + userId + ":" + LocalDate.now();



        // 关键改动：安全地获取并转换为 Long 类型
        // redisTemplate.opsForValue().get(key) 返回的是 Object，
        // 可能是 Long、Integer 或其他数字类型，取决于 Redis 的存储和序列化器。
        // 为了安全转换，我们先获取为 Object，再判断或转换为 Long。
        Object rawUsedCount = redisTemplate.opsForValue().get(dailyDrawCountKey);
        Long usedCount = 0L; // 默认值


        if (rawUsedCount instanceof Long) {
            usedCount = (Long) rawUsedCount;
        } else if (rawUsedCount instanceof Integer) {
            usedCount = ((Integer) rawUsedCount).longValue(); // 将 Integer 转换为 Long
        } else if (rawUsedCount instanceof String) {
            // 如果 RedisTemplate 配置了 StringRedisSerializer 并且存储的是数字字符串
            try {
                usedCount = Long.parseLong((String) rawUsedCount);
            } catch (NumberFormatException e) {
                // 处理解析错误，例如日志记录
                System.err.println("从Redis获取的抽奖次数无法解析为数字: " + rawUsedCount);
            }
        }
        // 如果 rawUsedCount 为 null，usedCount 保持默认的 0L

        int maxDailyDraws = 3; // 保持与 LotteryService 中一致

        response.put("code", 200);
        response.put("message", "查询成功");
        // 返回最大次数、已用次数和剩余次数
        response.put("data", Map.of(
                "max", maxDailyDraws,
                "used", usedCount.intValue(),
                "remaining", Math.max(0, maxDailyDraws - usedCount.intValue()) // 确保剩余次数不为负
        ));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }






}

package com.miku.lottery.controller;

// 1. 删掉 import javax.annotation.Resource;
// 2. 引入 Autowired
import org.springframework.beans.factory.annotation.Autowired;
import com.miku.lottery.entity.Prize;
import com.miku.lottery.service.LotteryService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/lottery")
public class LotteryController {

    @Autowired // 3. 将 @Resource 修改为 @Autowired
    private LotteryService lotteryService;

    // ... 其他代码不变 ...
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
}

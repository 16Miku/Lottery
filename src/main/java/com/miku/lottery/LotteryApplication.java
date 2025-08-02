package com.miku.lottery;

import org.mybatis.spring.annotation.MapperScan; // 1. 导入这个包
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.miku.lottery.mapper") // 2. 添加这一行，指向你的 mapper 包
public class LotteryApplication {

    public static void main(String[] args) {
        SpringApplication.run(LotteryApplication.class, args);
    }

}

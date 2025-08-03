package com.miku.lottery.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry; // 导入 CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer; // 导入 WebMvcConfigurer

/**
 * Web 配置类，用于配置 CORS (跨域资源共享)。
 * 允许前端 Vue 应用访问后端 API。
 */
@Configuration // 声明这是一个 Spring 配置类
public class WebConfig implements WebMvcConfigurer {

    /**
     * 配置 CORS 规则
     * @param registry CORS 注册表
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 对所有路径（/**）应用 CORS 规则
                .allowedOrigins("http://localhost:5173") // 允许来自 http://localhost:5173 的请求
                // 在生产环境中，这里应该是你的前端域名
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 允许的 HTTP 方法
                .allowedHeaders("*") // 允许所有请求头
                .allowCredentials(true) // 允许发送 Cookie 和 HTTP 认证信息
                .maxAge(3600); // 预检请求（OPTIONS）的缓存时间，单位秒
    }
}

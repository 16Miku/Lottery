package com.miku.lottery.config;

import com.miku.lottery.filter.JwtAuthenticationFilter; // 导入 JWT 过滤器
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity; // 导入 HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity; // 导入 EnableWebSecurity
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer; // 导入 AbstractHttpConfigurer
import org.springframework.security.config.http.SessionCreationPolicy; // 导入 SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain; // 导入 SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter; // 导入 UsernamePasswordAuthenticationFilter

/**
 * Spring Security 配置类
 * 定义安全规则、认证方式和过滤器链。
 */
@Configuration // 声明为配置类
@EnableWebSecurity // 启用 Spring Security 的 Web 安全功能
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter; // 注入 JWT 认证过滤器

    /**
     * 配置安全过滤器链
     * @param http HttpSecurity 配置对象
     * @return SecurityFilterChain 实例
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 禁用 CSRF 防护，因为我们使用 JWT，不依赖 Session 和 CSRF Token
                .csrf(AbstractHttpConfigurer::disable)
                // 配置会话管理策略为无状态，因为 JWT 是无状态的
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 配置请求授权规则
                .authorizeHttpRequests(authorize -> authorize
                        // 允许对注册和登录接口的匿名访问
                        .requestMatchers("/auth/**", "/").permitAll() // 允许 /auth/* 和根路径（前端页面）匿名访问
                        // 允许对静态资源的匿名访问
                        .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                        // 其他所有请求都需要认证
                        .anyRequest().authenticated()
                )
                // 在 UsernamePasswordAuthenticationFilter 之前添加 JWT 认证过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build(); // 构建并返回 SecurityFilterChain
    }
}

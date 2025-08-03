package com.miku.lottery.filter;

import com.miku.lottery.entity.User;
import com.miku.lottery.service.UserService; // 导入 UserService
import com.miku.lottery.util.JwtUtil; // 导入 JwtUtil
import jakarta.servlet.FilterChain; // 导入 FilterChain
import jakarta.servlet.ServletException; // 导入 ServletException
import jakarta.servlet.http.HttpServletRequest; // 导入 HttpServletRequest
import jakarta.servlet.http.HttpServletResponse; // 导入 HttpServletResponse
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken; // 导入 UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder; // 导入 SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails; // 导入 UserDetails
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource; // 导入 WebAuthenticationDetailsSource
import org.springframework.stereotype.Component; // 导入 Component
import org.springframework.web.filter.OncePerRequestFilter; // 导入 OncePerRequestFilter

import java.io.IOException;

/**
 * JWT 认证过滤器
 * 负责在每个请求前从 Header 中解析 JWT Token，并进行用户认证。
 */
@Component // 声明为 Spring 组件
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private UserService userService; // 注入 UserService，用于加载用户详情

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. 从请求头中获取 Authorization 字段
        final String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String jwt = null;

        // 2. 检查 Authorization 头是否有效，并提取 JWT
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            // "Bearer " 后面的部分即为 Token
            try {
                username = JwtUtil.extractUsername(jwt);
                // 尝试从 Token 中提取用户名
            } catch (Exception e) {
                // Token 过期、签名错误等，在这里捕获异常
                System.err.println("JWT Token 解析失败: " + e.getMessage());
            }
        }

        // 3. 如果成功提取到用户名，并且当前 SecurityContext 中没有认证信息
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // 4. 从 UserService 加载用户详情（这里我们使用 User 实体作为 UserDetails 的简化）
            // 实际生产中，通常会有一个 UserDetailsService 来加载 Spring Security 的 UserDetails
            User user = userService.findByUsername(username);

            // 5. 验证 Token 的有效性
            if (user != null && JwtUtil.validateToken(jwt)) {
                // 6. 构建认证对象
                // UsernamePasswordAuthenticationToken 实现了 Authentication 接口
                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                        new UsernamePasswordAuthenticationToken(user, null, null); // 暂时不设置权限

                // 设置认证详情，包括请求的 IP 地址和 Session ID 等
                usernamePasswordAuthenticationToken
                        .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 7. 将认证对象设置到 Spring Security Context 中
                // 这表示当前请求已被认证，后续的 Spring Security 过滤器将不会再次拦截认证
                SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
            }
        }
        // 8. 继续过滤器链
        filterChain.doFilter(request, response);
    }
}

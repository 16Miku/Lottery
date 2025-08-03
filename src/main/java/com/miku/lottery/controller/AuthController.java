package com.miku.lottery.controller;

import com.miku.lottery.entity.User;
import com.miku.lottery.service.UserService;
import com.miku.lottery.util.JwtUtil; // 导入 JWT 工具类
import lombok.Data; // 导入 Lombok 的 Data 注解
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus; // 导入 HttpStatus
import org.springframework.http.ResponseEntity; // 导入 ResponseEntity
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody; // 导入 RequestBody
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户认证控制器
 * 负责用户注册和登录的 API 接口。
 */
@RestController
@RequestMapping("/auth") // 认证接口的根路径
public class AuthController {

    @Autowired
    private UserService userService; // 注入 UserService

    /**
     * 用户注册请求体
     */
    @Data // Lombok 注解，自动生成 getter, setter
    static class RegisterRequest {
        private String username;
        private String password;
    }

    /**
     * 用户登录请求体
     */
    @Data // Lombok 注解，自动生成 getter, setter
    static class LoginRequest {
        private String username;
        private String password;
    }

    /**
     * 用户注册接口
     * @param request 包含用户名和密码的请求体
     * @return 注册结果
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest request) {
        Map<String, Object> response = new HashMap<>();
        if (request.getUsername() == null || request.getUsername().isEmpty() ||
                request.getPassword() == null || request.getPassword().isEmpty()) {
            response.put("code", 400);
            response.put("message", "用户名或密码不能为空。");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        User registeredUser = userService.register(request.getUsername(), request.getPassword());
        if (registeredUser != null) {
            response.put("code", 200);
            response.put("message", "注册成功！");
            // 注册成功后，可以考虑直接返回 Token 或提示用户登录
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            response.put("code", 409); // Conflict
            response.put("message", "注册失败，用户名可能已存在。");
            return new ResponseEntity<>(response, HttpStatus.CONFLICT);
        }
    }

    /**
     * 用户登录接口
     * @param request 包含用户名和密码的请求体
     * @return 登录结果，成功则返回 JWT Token
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        Map<String, Object> response = new HashMap<>();
        if (request.getUsername() == null || request.getUsername().isEmpty() ||
                request.getPassword() == null || request.getPassword().isEmpty()) {
            response.put("code", 400);
            response.put("message", "用户名或密码不能为空。");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        User user = userService.login(request.getUsername(), request.getPassword());
        if (user != null) {
            // 登录成功，生成 JWT Token
            String token = JwtUtil.generateToken(user.getUsername(), user.getId());
            response.put("code", 200);
            response.put("message", "登录成功！");
            response.put("data", Map.of("token", token, "username", user.getUsername(), "userId", user.getId()));
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            response.put("code", 401); // Unauthorized
            response.put("message", "登录失败，用户名或密码错误。");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }
    }
}

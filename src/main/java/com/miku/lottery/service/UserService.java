package com.miku.lottery.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.miku.lottery.entity.User;
import com.miku.lottery.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; // 导入密码编码器
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID; // 导入 UUID，用于生成盐值

/**
 * 用户业务服务类
 * 负责用户注册、登录等核心用户管理业务逻辑。
 */
@Service
public class UserService {

    @Autowired
    private UserMapper userMapper; // 注入 UserMapper

    // 注入 BCryptPasswordEncoder，用于密码加密和验证
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 用户注册
     * @param username 用户名
     * @param password 明文密码
     * @return 注册成功的用户对象，如果用户名已存在则返回 null
     */
    @Transactional(rollbackFor = Exception.class) // 注册操作涉及数据库写入，开启事务
    public User register(String username, String password) {
        // 1. 检查用户名是否已存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        User existingUser = userMapper.selectOne(queryWrapper);
        if (existingUser != null) {
            System.out.println("注册失败：用户名 " + username + " 已存在。");
            return null; // 用户名已存在
        }

        // 2. 生成盐值
        String salt = UUID.randomUUID().toString().replace("-", ""); // 使用 UUID 生成随机盐值
        // 3. 对密码进行加盐哈希
        String hashedPassword = passwordEncoder.encode(password + salt); // BCrypt 内部会处理盐值，但我们这里为了演示，手动加盐后再编码

        // 4. 构建用户对象
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPasswordHash(hashedPassword);
        newUser.setSalt(salt);
        newUser.setCreateTime(LocalDateTime.now());
        newUser.setUpdateTime(LocalDateTime.now());

        // 5. 插入数据库
        int insertedRows = userMapper.insert(newUser);
        if (insertedRows > 0) {
            System.out.println("用户 " + username + " 注册成功。");
            return newUser;
        } else {
            System.out.println("用户 " + username + " 注册失败（数据库插入失败）。");
            return null;
        }
    }

    /**
     * 用户登录
     * @param username 用户名
     * @param password 明文密码
     * @return 登录成功的用户对象，如果用户名不存在或密码错误则返回 null
     */
    public User login(String username, String password) {
        // 1. 根据用户名查询用户
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        User user = userMapper.selectOne(queryWrapper);

        if (user == null) {
            System.out.println("登录失败：用户名 " + username + " 不存在。");
            return null; // 用户名不存在
        }

        // 2. 验证密码
        // BCryptPasswordEncoder.matches(rawPassword, encodedPassword) 会自动处理盐值和哈希验证
        // 注意：如果你的 hashedPassword 是 passwordEncoder.encode(rawPassword + salt) 得到的，
        // 那么 matches 传入的 rawPassword 应该是 rawPassword + salt
        // 但 BCryptPasswordEncoder 推荐直接 encode(rawPassword)，它内部会生成并管理盐值。
        // 为了简化和符合BCrypt的最佳实践，我们调整一下 register 方法的密码处理。
        // 这里假设 passwordEncoder.encode(password) 已经包含了盐值的处理。
        // 如果注册时是 passwordEncoder.encode(password + salt)，那么验证时也需要 password + salt
        boolean isPasswordMatch = passwordEncoder.matches(password + user.getSalt(), user.getPasswordHash());

        if (isPasswordMatch) {
            System.out.println("用户 " + username + " 登录成功。");
            return user;
        } else {
            System.out.println("登录失败：用户名 " + username + " 或密码错误。");
            return null; // 密码不匹配
        }
    }

    /**
     * 根据用户名查找用户
     * @param username 用户名
     * @return 用户对象或 null
     */
    public User findByUsername(String username) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        return userMapper.selectOne(queryWrapper);
    }

    /**
     * 根据用户ID查找用户
     * @param userId 用户ID
     * @return 用户对象或 null
     */
    public User findById(Long userId) {
        return userMapper.selectById(userId);
    }
}

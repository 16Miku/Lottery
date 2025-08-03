package com.miku.lottery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable; // 导入 Serializable 接口
import java.time.LocalDateTime;

import org.springframework.security.core.GrantedAuthority; // 导入 GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails; // 导入 UserDetails


import java.util.Collection; // 导入 Collection
import java.util.Collections; // 导入 Collections

/**
 * 用户实体类，映射数据库 user 表
 */
@Data // Lombok 注解，自动生成 getter, setter, toString 等方法
@TableName("user") // 映射到数据库的 "user" 表
public class User implements Serializable, UserDetails {
    // 实现 Serializable 接口，以备将来缓存需求
    // 实现 UserDetails 接口


    private static final long serialVersionUID = 1L; // 序列化版本号，推荐添加

    @TableId(type = IdType.AUTO) // 声明主键并设置为数据库自增
    private Long id; // 用户唯一标识符

    private String username; // 用户名

    private String passwordHash; // 密码哈希值

    private String salt; // 盐值

    private LocalDateTime createTime; // 创建时间

    private LocalDateTime updateTime; // 更新时间



    // --- UserDetails 接口实现 ---

    /**
     * 返回授予用户的权限。
     * 暂时返回空集合，表示没有特定权限。
     * 实际应用中，这里会根据用户的角色（如 ADMIN, USER）返回相应的 GrantedAuthority 集合。
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList(); // 暂时没有角色/权限
    }

    /**
     * 返回用于认证的密码。
     * 注意：这里返回的是哈希后的密码。
     */
    @Override
    public String getPassword() {
        return this.passwordHash;
    }

    /**
     * 返回用户的用户名。
     */
    @Override
    public String getUsername() {
        return this.username;
    }

    /**
     * 指示用户的帐户是否已过期。
     * 这里始终返回 true，表示未过期。
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * 指示用户是否被锁定或解锁。
     * 这里始终返回 true，表示未锁定。
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * 指示用户的凭据（密码）是否已过期。
     * 这里始终返回 true，表示未过期。
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * 指示用户是否启用。
     * 这里始终返回 true，表示已启用。
     */
    @Override
    public boolean isEnabled() {
        return true;
    }



}

package com.miku.lottery.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule; // 导入 JavaTimeModule

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration; // 导入 RedisCacheConfiguration
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer; // 使用 Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext; // 导入 RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer; // 导入 StringRedisSerializer

import java.time.Duration; // 导入 Duration 类，用于设置缓存过期时间

/**
 * Redis 配置类，用于自定义 RedisTemplate 和 RedisCacheManager 的序列化器。
 */
@Configuration // 声明这是一个配置类，会被 Spring 容器扫描并加载
public class RedisConfig {

    /**
     * 配置 RedisTemplate，自定义键和值的序列化器。
     * 这个 Bean 主要用于当你在代码中直接注入和使用 RedisTemplate 时，
     * 它的序列化行为。@Cacheable 注解默认不直接使用这个 RedisTemplate 的序列化器，
     * 而是通过 RedisCacheConfiguration 来配置。
     *
     * @param redisConnectionFactory Redis 连接工厂，由 Spring 自动注入
     * @return 配置好的 RedisTemplate 实例
     */
    @Bean // 声明这是一个 Spring Bean，会被 Spring 容器管理
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        // 创建并配置 ObjectMapper，用于 JSON 序列化和反序列化
        ObjectMapper objectMapper = new ObjectMapper();
        // 指定序列化时可以访问所有字段（包括私有字段），以及所有 getter/setter 方法
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // 注册 JavaTimeModule 模块，解决 LocalDateTime 等 Java 8 时间类型序列化问题
        objectMapper.registerModule(new JavaTimeModule());
        // 激活默认类型信息，解决反序列化时多态类型的问题（通常用于存储复杂对象）
        // 对于 Prize 这种简单对象，如果 Prize 是 final 类且不涉及继承，此行可能不是必需的，但保留可增强兼容性。
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);

        // 使用 Jackson2JsonRedisSerializer，并传入自定义的 ObjectMapper 实例
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);

        // 设置键（Key）的序列化器为 StringRedisSerializer
        template.setKeySerializer(new StringRedisSerializer());
        // 设置值（Value）的序列化器为 Jackson2JsonRedisSerializer
        template.setValueSerializer(jackson2JsonRedisSerializer);

        // 设置 Hash 类型键（HashKey）的序列化器为 StringRedisSerializer
        template.setHashKeySerializer(new StringRedisSerializer());
        // 设置 Hash 类型值（HashValue）的序列化器为 Jackson2JsonRedisSerializer
        template.setHashValueSerializer(jackson2JsonRedisSerializer);

        // 调用 afterPropertiesSet() 方法，确保所有属性设置完毕并完成初始化
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 配置 Redis 缓存管理器（RedisCacheManager）的默认缓存行为。
     * 这是 @Cacheable/@CacheEvict 注解能够使用 JSON 序列化的关键。
     *
     * @return 配置好的 RedisCacheConfiguration 实例
     */
    @Bean
    public RedisCacheConfiguration redisCacheConfiguration() {
        // 创建并配置 ObjectMapper，用于 JSON 序列化和反序列化
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.registerModule(new JavaTimeModule()); // 解决 Java 8 时间类型序列化问题
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);

        // 使用 Jackson2JsonRedisSerializer 作为缓存值的序列化器
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);

        return RedisCacheConfiguration.defaultCacheConfig() // 获取默认的缓存配置
                .entryTtl(Duration.ofMinutes(5)) // 设置缓存项的默认过期时间为 5 分钟
                // 设置键的序列化器为 StringRedisSerializer
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                // 设置值的序列化器为 Jackson2JsonRedisSerializer
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer));
    }
}

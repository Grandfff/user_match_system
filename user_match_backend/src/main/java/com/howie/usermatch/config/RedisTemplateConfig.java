package com.howie.usermatch.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * 自定义 Redis 序列化配置
 */
@Configuration
public class RedisTemplateConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        // 创建 RedisTemplate 对象
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        // 设置连接工厂
        redisTemplate.setConnectionFactory(connectionFactory);
        // 设置 Key 的序列化（String 序列化，避免 key 出现乱码前缀）
        redisTemplate.setKeySerializer(RedisSerializer.string());

        // 创建 JSON 序列化工具
        GenericJackson2JsonRedisSerializer jsonRedisSerializer = new GenericJackson2JsonRedisSerializer();
        // 设置 Value 的序列化（JSON 序列化）
        redisTemplate.setValueSerializer(jsonRedisSerializer);
        // 设置 Hash 的 Key 序列化（String 序列化）
        redisTemplate.setHashKeySerializer(RedisSerializer.string());
        // 设置 Hash 的 Value 序列化（JSON 序列化）
        redisTemplate.setHashValueSerializer(jsonRedisSerializer);

        return redisTemplate;
    }
}

package com.howie.usermatch.config;

import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 配置（用于分布式锁）
 */
@Configuration
@ConfigurationProperties(prefix = "spring.redis")
@Data
public class RedissonConfig {

    private String host;

    private String port;

    private String password;

    private int database;

    @Bean
    public RedissonClient redissonClient() {
        // 1. 创建配置
        Config config = new Config();
        String redisAddress = String.format("redis://%s:%s", host, port);
        // 使用单个 Redis（没有开集群 useClusterServers()），设置地址、使用库和密码
        config.useSingleServer()
                .setAddress(redisAddress)
                .setDatabase(database)
                .setPassword(password);
        // 2. 创建实例
        return Redisson.create(config);
    }
}

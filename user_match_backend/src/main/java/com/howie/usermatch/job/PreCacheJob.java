package com.howie.usermatch.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.howie.usermatch.model.domain.User;
import com.howie.usermatch.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.howie.usermatch.contant.UserConstant.USER_RECOMMEND_KEY;

/**
 * 数据预热定时任务
 * <p>
 * 定时将推荐用户数据预热到 Redis，加速访问速度；
 * 通过 Redisson 分布式锁保证多台服务器同时执行时只有一台真正执行预热。
 */
@Component
@Slf4j
public class PreCacheJob {

    @Resource
    private UserService userService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RedissonClient redissonClient;

    /**
     * 推荐接口缓存过期时间（毫秒）
     */
    private static final long RECOMMEND_CACHE_TTL = 30000L;

    /**
     * 重点用户（预热这些用户的推荐数据；0 表示未登录默认首页）
     */
    private List<Long> mainUserList = Arrays.asList(0L, 1L);

    /**
     * 每天执行，预热推荐用户
     */
    @Scheduled(cron = "0 12 1 * * *") // 自己设置时间测试
    public void doCacheRecommendUser() {
        RLock lock = redissonClient.getLock("howie:precachejob:docache:lock");
        try {
            // 只有一个线程能获取到锁，避免多台服务器重复预热
            if (lock.tryLock(0, -1, TimeUnit.MILLISECONDS)) {
                log.info("getLock: {}", Thread.currentThread().getId());
                for (Long userId : mainUserList) {
                    // 查数据库（预热第一页推荐数据）
                    QueryWrapper<User> queryWrapper = new QueryWrapper<>();
                    Page<User> userPage = userService.page(new Page<>(1, 8), queryWrapper);
                    // 脱敏，避免把密码写进缓存
                    List<User> safetyUsers = userPage.getRecords().stream()
                            .map(userService::getSafetyUser)
                            .collect(Collectors.toList());
                    userPage.setRecords(safetyUsers);
                    // key 与 /user/recommend 的分页缓存 key 保持一致：{userId}:{pageNum}:{pageSize}
                    String redisKey = USER_RECOMMEND_KEY + userId + ":1:8";
                    ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
                    // 写缓存，设置过期时间
                    try {
                        valueOperations.set(redisKey, userPage, RECOMMEND_CACHE_TTL, TimeUnit.MILLISECONDS);
                    } catch (Exception e) {
                        log.error("redis set key error", e);
                    }
                }
            }
        } catch (InterruptedException e) {
            log.error("doCacheRecommendUser error", e);
        } finally {
            // 只能释放自己的锁
            if (lock.isHeldByCurrentThread()) {
                log.info("unLock: {}", Thread.currentThread().getId());
                lock.unlock();
            }
        }
    }
}

package com.howie.usermatch;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * Redisson 测试：分布式锁 & Redis 数据增删改查
 */
@SpringBootTest
public class RedissonTest {

    @Resource
    private RedissonClient redissonClient;

    /**
     * 测试 Redisson 对 Redis 数据的增删改查（RBucket 字符串）
     */
    @Test
    public void testRedissonCrud() {
        RBucket<String> bucket = redissonClient.getBucket("howie:test:redisson:crud");
        // 增（set）
        bucket.set("hello redisson");
        // 查（get）
        Assertions.assertEquals("hello redisson", bucket.get());
        System.out.println("get = " + bucket.get());
        // 改（重新 set）
        bucket.set("hello redisson updated");
        Assertions.assertEquals("hello redisson updated", bucket.get());
        System.out.println("updated = " + bucket.get());
        // 删（delete）
        bucket.delete();
        Assertions.assertNull(bucket.get());
        System.out.println("after delete = " + bucket.get());
    }

    /**
     * 测试 Redisson 分布式锁
     */
    @Test
    public void testRedissonLock() throws InterruptedException {
        RLock lock = redissonClient.getLock("howie:test:redisson:lock");
        boolean isLock = lock.tryLock(0, -1, TimeUnit.MILLISECONDS);
        try {
            if (isLock) {
                System.out.println("getLock: " + Thread.currentThread().getId());
                // 模拟执行业务逻辑
                Thread.sleep(1000);
            }
        } finally {
            // 只能释放自己的锁
            if (lock.isHeldByCurrentThread()) {
                System.out.println("unLock: " + Thread.currentThread().getId());
                lock.unlock();
            }
        }
        Assertions.assertTrue(isLock);
    }
}

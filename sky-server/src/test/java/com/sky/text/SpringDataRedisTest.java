package com.sky.text;

import com.sky.config.RedisConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

//@SpringBootTest
public class SpringDataRedisTest {
    @Autowired
    private RedisTemplate redisTemplate;
    @Test
    public void testRedis() {
        System.out.println(redisTemplate);
    }
    /**
     * 操作字符串类型的数据
     */
    @Test
    public void testString() {
        //set get setex setnx
        redisTemplate.opsForValue().set("name", "张三");
        System.out.println(redisTemplate.opsForValue().get("name"));
        redisTemplate.opsForValue().set("code", "12345678", 3, TimeUnit.MINUTES);
        System.out.println(redisTemplate.opsForValue().get("code"));
        redisTemplate.opsForValue().setIfAbsent("lock","1");
        redisTemplate.opsForValue().setIfAbsent("lock","2");
    }
    /**
     * 操作Hash类型数据
     */
    @Test
    public void testHash() {
        //hset hget  hdel hkeys hvals
        HashOperations hashOperations = redisTemplate.opsForHash();
        hashOperations.put("100", "name", "张三");
        hashOperations.put("100", "age", "20");
        System.out.println(hashOperations.get("100", "name"));

        System.out.println(hashOperations.keys("100"));
        System.out.println(hashOperations.values("100"));
        hashOperations.delete("100", "name");
    }
    /**
     * 操作List类型数据
     */
    @Test
    public void testList() {
        //lpush lpop lrange lrem
        redisTemplate.opsForList().leftPush("list", "1");
        redisTemplate.opsForList().leftPush("list", "2");
        redisTemplate.opsForList().leftPush("list", "3");
        System.out.println(redisTemplate.opsForList().leftPop("list"));
        System.out.println(redisTemplate.opsForList().range("list", 0, -1));
    }
}

package com.ren.service.impl;

import com.alibaba.fastjson.JSON;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.ren.service.CacheService;
import com.ren.service.model.RedisLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.sleep;

/**
 * @author Ren
 */

@Service
public class CacheServiceImpl implements CacheService {

    private Cache<String, Object> commonCache = null;


    @PostConstruct  //加上次注解，spring加载时会优先加载此方法
    public void init() {  //初始化配置
        commonCache = CacheBuilder.newBuilder()
                // 设置缓存容器的初始容量为10
                .initialCapacity(10)
                // 设置缓存中最大可以存储100个Key  超过100个key之后就会按照LRU的策略移除缓存项
                .maximumSize(100)
                // 设置写缓存后多少秒过期
                .expireAfterWrite(60, TimeUnit.SECONDS).build();
    }

    @Override
    public void setCommonCache(String key, Object value) {
        commonCache.put(key, value);
    }

    @Override
    public Object getFromCommonCache(String key) {

        // 如果存在的话返回结果 不存在则返回null
        return commonCache.getIfPresent(key);
    }


    /*public String get(String key) throws InterruptedException {
        String value = redisTemplate.opsForValue(key);
        if (value == null) { //代表缓存值过期
            //设置3min的超时，防止del操作失败的时候，下次缓存过期一直不能load db
            if (redisTemplate.opsForSet(key_mutex, 1, 3 * 60) == 1) {  //代表设置成功
                value = db.get(key);
                redisTemplate.opsForSet(key, value, expire_secs);
                redisTemplate.del(key_mutex);
            } else {  //这个时候代表同时候的其他线程已经load db并回设到缓存了，这时候重试获取缓存值即可
                sleep(50);
                get(key);  //重试
            }
        } else {
            return value;
        }
        return value;
    }*/

/*

    public RedisLock getLock(String key,long timeOut,long tryLockTimeout) {
        long timestamp = System.currentTimeMillis();
        try {
            key = getKey(key);
            UUID uuid = UUID.randomUUID();
            while ((System.currentTimeMillis() - timestamp) < tryLockTimeout) {
                // 参数分别对应了脚本、key序列化工具、value序列化工具，后面参数对应scriptLock字符串中的三个变量值，
                // KEYS[1]，ARGV[1]，ARGV[2]，含义为锁的key，key对应的value，以及key 的存在时间(单位毫秒)
                String result = (String) redisTemplate.execute(scriptLock,
                        redisTemplate.getStringSerializer(),
                        redisTemplate.getStringSerializer(),
                        Collections.singletonList(key),
                        uuid.toString(),
                        String.valueOf(timeOut));
                if (result != null && result.equals("OK")) {
                    return new RedisLock(key, uuid.toString());
                } else {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error(JSON.toJSONString(e.getStackTrace()));
        }
        return null;
    }



    */
/**
 * 释放锁
 * @param lock
 *//*

    public void releaseLock(RedisLock lock) {
        Object execute = redisTemplate.execute(scriptLock2,
                redisTemplate.getStringSerializer(),
                redisTemplate.getStringSerializer(),
                Collections.singletonList(lock.getKey()),
                lock.getValue()
        );
        // 当返回0的时候可能因为超时而锁已经过期
        if (new Integer("1").equals(execute)) {
            log.info("释放锁");
        }
    }
*/


}

package com.ren.service.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.ren.service.CacheService;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * @author Ren
 */

@Service
public class CacheServiceImpl implements CacheService {

    private Cache<String,Object> commonCache = null;

    @PostConstruct  //加上次注解，spring加载时会优先加载此方法
    public void init(){  //初始化配置
        commonCache = CacheBuilder.newBuilder()
                // 设置缓存容器的初始容量为10
                .initialCapacity(10)
                // 设置缓存中最大可以存储100个Key  超过100个key之后就会按照LRU的策略移除缓存项
                .maximumSize(100)
                // 设置写缓存后多少秒过期
                .expireAfterWrite(60,TimeUnit.SECONDS).build();
    }


    @Override
    public void setCommonCache(String key, Object value) {
        commonCache.put(key,value);
    }

    @Override
    public Object getFromCommonCache(String key) {

        // 如果存在的话返回结果 不存在则返回null
        return commonCache.getIfPresent(key);
    }
}

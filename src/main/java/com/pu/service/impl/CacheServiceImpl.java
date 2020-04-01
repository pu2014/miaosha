package com.pu.service.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.pu.service.ICacheService;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * Description:
 * Created By @Author my on @Date 2020/4/1 23:48
 */
@Service
public class CacheServiceImpl implements ICacheService {

    private Cache<String, Object> commonCache = null;

    @PostConstruct
    public void init(){
        commonCache = CacheBuilder.newBuilder()
                //设置缓存的初始容量
                .initialCapacity(10)
                //设置缓存的最大可以存储的100个key，超过100个就会按照LRU的策略移除缓存项
                .maximumSize(100)
                //设置过期时间
                .expireAfterWrite(60, TimeUnit.SECONDS).build();
    }

    @Override
    public void setCommonCache(String key, Object value) {
        commonCache.put(key, value);
    }

    @Override
    public Object getFromCommonCache(String key) {
        return commonCache.getIfPresent(key);
    }
}

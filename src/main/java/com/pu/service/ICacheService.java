package com.pu.service;

/**
 * Description:封装本地缓存操作类
 * Created By @Author my on @Date 2020/4/1 23:45
 */
public interface ICacheService {
    //存方法
    void setCommonCache(String key, Object value);
    //取方法
    Object getFromCommonCache(String key);
}

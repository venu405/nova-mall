package com.novamall.api.cache;

import java.util.List;

/**
 * @apiNote 热点数据缓存抽象，Redis 实现与空实现通过 novamall.cache.enabled 条件装配
 */
public interface NovaMallCacheService {

    /**
     * 读取缓存，未命中或缓存不可用时返回 null
     */
    <T> T get(String key, Class<T> clazz);

    /**
     * 读取 List 类型缓存，未命中或缓存不可用时返回 null；命中空 List 视为有效的"无数据"标记
     */
    <T> List<T> getList(String key, Class<T> clazz);

    /**
     * 写入缓存并设置过期时间(秒)
     */
    void set(String key, Object value, long timeoutSeconds);

    /**
     * 删除指定 key
     */
    void delete(String key);

    /**
     * 按前缀模式删除 key，用于列表类缓存的批量失效
     */
    void deleteByPattern(String pattern);
}

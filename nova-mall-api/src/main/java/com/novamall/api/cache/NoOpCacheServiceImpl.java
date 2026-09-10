package com.novamall.api.cache;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @apiNote 缓存空实现，novamall.cache.enabled=false 时装配，所有操作为空转，保证无 Redis 环境正常运行
 */
@Service
@ConditionalOnProperty(name = "novamall.cache.enabled", havingValue = "false")
public class NoOpCacheServiceImpl implements NovaMallCacheService {

    @Override
    public <T> T get(String key, Class<T> clazz) {
        return null;
    }

    @Override
    public <T> List<T> getList(String key, Class<T> clazz) {
        return null;
    }

    @Override
    public void set(String key, Object value, long timeoutSeconds) {
    }

    @Override
    public void delete(String key) {
    }

    @Override
    public void deleteByPattern(String pattern) {
    }
}

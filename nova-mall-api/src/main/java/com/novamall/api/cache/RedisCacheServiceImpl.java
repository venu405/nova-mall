package com.novamall.api.cache;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @apiNote 基于 Redis 的缓存实现，key 与 value 均为 String，value 由 Jackson 序列化为 JSON；
 * 所有操作捕获异常并降级（缓存不可用时直接穿透到数据库，不影响主流程）
 */
@Service
@ConditionalOnProperty(name = "novamall.cache.enabled", havingValue = "true", matchIfMissing = true)
public class RedisCacheServiceImpl implements NovaMallCacheService {

    private static final Logger logger = LoggerFactory.getLogger(RedisCacheServiceImpl.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public <T> T get(String key, Class<T> clazz) {
        try {
            String value = stringRedisTemplate.opsForValue().get(key);
            if (!StringUtils.hasText(value)) {
                return null;
            }
            return OBJECT_MAPPER.readValue(value, clazz);
        } catch (Exception e) {
            logger.warn("缓存读取失败，降级查询数据库，key={}", key, e);
            return null;
        }
    }

    @Override
    public <T> List<T> getList(String key, Class<T> clazz) {
        try {
            String value = stringRedisTemplate.opsForValue().get(key);
            if (!StringUtils.hasText(value)) {
                return null;
            }
            JavaType javaType = OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, clazz);
            return OBJECT_MAPPER.readValue(value, javaType);
        } catch (Exception e) {
            logger.warn("缓存读取失败，降级查询数据库，key={}", key, e);
            return null;
        }
    }

    @Override
    public void set(String key, Object value, long timeoutSeconds) {
        try {
            stringRedisTemplate.opsForValue().set(key, OBJECT_MAPPER.writeValueAsString(value), timeoutSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.warn("缓存写入失败，忽略，key={}", key, e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            stringRedisTemplate.delete(key);
        } catch (Exception e) {
            logger.warn("缓存删除失败，忽略，key={}", key, e);
        }
    }

    @Override
    public void deleteByPattern(String pattern) {
        try {
            Set<String> keys = stringRedisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
            }
        } catch (Exception e) {
            logger.warn("缓存批量删除失败，忽略，pattern={}", pattern, e);
        }
    }
}

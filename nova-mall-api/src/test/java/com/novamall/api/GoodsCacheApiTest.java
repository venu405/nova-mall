package com.novamall.api;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 商品详情缓存：首次查询回源写缓存、二次查询命中缓存；
 * 查询不存在的商品返回业务错误并写入 TTL 不超过 60 秒的空值缓存（防穿透）
 */
class GoodsCacheApiTest extends BaseApiTest {

    private static final String GOODS_CACHE_KEY = "novamall:goods:detail:" + TEST_GOODS_ID;

    @Test
    void goodsDetailFirstQueryWritesCache() {
        String token = registerAndLogin(randomPhone(), "123456");
        resetGoods(TEST_GOODS_ID, 100);

        Map<String, Object> firstQuery = getForMap("/api/v1/goods/detail/" + TEST_GOODS_ID, token);
        assertEquals(200, firstQuery.get("resultCode"));
        // 第一次查询回源数据库后应写入缓存
        assertTrue(Boolean.TRUE.equals(stringRedisTemplate.hasKey(GOODS_CACHE_KEY)), "首次查询后应写入商品详情缓存");

        Map<String, Object> secondQuery = getForMap("/api/v1/goods/detail/" + TEST_GOODS_ID, token);
        assertEquals(200, secondQuery.get("resultCode"));
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) secondQuery.get("data");
        assertNotNull(data);
        assertNotNull(data.get("goodsName"), "命中缓存的详情数据应完整");

        stringRedisTemplate.delete(GOODS_CACHE_KEY);
    }

    @Test
    void queryNonExistGoodsWritesNullCache() {
        String token = registerAndLogin(randomPhone(), "123456");
        long nonExistGoodsId = 99999999L;
        String cacheKey = "novamall:goods:detail:" + nonExistGoodsId;
        stringRedisTemplate.delete(cacheKey);

        Map<String, Object> result = getForMap("/api/v1/goods/detail/" + nonExistGoodsId, token);
        assertEquals(500, result.get("resultCode"));
        assertEquals("商品不存在！", result.get("message"));

        // 空值缓存已写入且 TTL 不超过 60 秒，防止恶意请求打穿数据库
        assertTrue(Boolean.TRUE.equals(stringRedisTemplate.hasKey(cacheKey)), "不存在的商品应写入空值缓存");
        Long ttl = stringRedisTemplate.getExpire(cacheKey, TimeUnit.SECONDS);
        assertNotNull(ttl);
        assertTrue(ttl > 0 && ttl <= 60, "空值缓存 TTL 应不超过 60 秒，实际：" + ttl);

        stringRedisTemplate.delete(cacheKey);
    }
}

package com.novamall.api.cache;

import java.util.concurrent.ThreadLocalRandom;

/**
 * @apiNote 缓存常量与过期时间策略
 */
public class CacheConstants {

    /** 商品详情缓存 key 前缀 */
    public final static String GOODS_DETAIL_KEY = "novamall:goods:detail:";

    /** 首页轮播图缓存 key 前缀 */
    public final static String INDEX_CAROUSEL_KEY = "novamall:index:carousel:";

    /** 首页配置商品缓存 key 前缀 */
    public final static String INDEX_CONFIG_GOODS_KEY = "novamall:index:config:";

    /** 商品 AI 简介缓存 key 前缀 */
    public final static String GOODS_AI_SUMMARY_KEY = "novamall:goods:ai-summary:";

    /** 商品 AI 简介缓存过期时间(秒)：24 小时，AI 生成内容对一致性不敏感 */
    public final static long AI_SUMMARY_TTL = 24 * 60 * 60L;

    /** 空值缓存过期时间(秒)：短 TTL，防止缓存穿透 */
    public final static long NULL_CACHE_TTL = 60L;

    /** 正常数据缓存基础过期时间(秒)：30 分钟 */
    public final static long CACHE_BASE_TTL = 30 * 60L;

    /** 过期时间随机抖动幅度(秒)：±5 分钟，防止缓存雪崩 */
    public final static long CACHE_TTL_JITTER = 5 * 60L;

    /**
     * 基础 TTL 上加随机抖动，避免大量 key 在同一时刻集中失效
     */
    public static long randomTtl() {
        return CACHE_BASE_TTL + ThreadLocalRandom.current().nextLong(-CACHE_TTL_JITTER, CACHE_TTL_JITTER + 1);
    }
}

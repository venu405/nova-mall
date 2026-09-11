package com.novamall.api;

import com.novamall.api.task.OrderTimeoutTask;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 订单超时自动取消：下单后将创建时间改为 40 分钟前，执行定时任务扫描，
 * 校验订单状态变为超时关闭、库存回补、商品详情缓存被清除
 */
class OrderTimeoutTest extends BaseApiTest {

    private static final String GOODS_CACHE_KEY = "novamall:goods:detail:" + TEST_GOODS_ID;

    @Autowired
    private OrderTimeoutTask orderTimeoutTask;

    @Test
    void timeoutOrderIsClosedAndStockRecovered() {
        String token = registerAndLogin(randomPhone(), "123456");
        resetGoods(TEST_GOODS_ID, 50);

        String orderNo = createOrder(token, TEST_GOODS_ID, 3);
        assertEquals(47, queryStock(TEST_GOODS_ID));

        // 先查询一次商品详情，让缓存存在，用于后续验证取消时缓存被清除
        assertResultSuccess(getForMap("/api/v1/goods/detail/" + TEST_GOODS_ID, token), "查询商品详情");
        assertTrue(Boolean.TRUE.equals(stringRedisTemplate.hasKey(GOODS_CACHE_KEY)));

        // 将订单创建时间改为 40 分钟前，使其超过 30 分钟支付超时阈值
        jdbcTemplate.update("update tb_novamall_order set create_time = ? where order_no = ?",
                new Timestamp(System.currentTimeMillis() - 40 * 60 * 1000L), orderNo);

        // 直接触发定时任务方法，执行扫描 → 取消 → 回补
        orderTimeoutTask.cancelTimeoutOrders();

        Integer orderStatus = jdbcTemplate.queryForObject(
                "select order_status from tb_novamall_order where order_no = ?", Integer.class, orderNo);
        assertEquals(-2, orderStatus, "超时订单应被关闭（状态 -2）");
        assertEquals(50, queryStock(TEST_GOODS_ID), "库存应回补到下单前的数量");
        assertFalse(Boolean.TRUE.equals(stringRedisTemplate.hasKey(GOODS_CACHE_KEY)), "商品详情缓存应被清除");
    }
}

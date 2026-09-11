package com.novamall.api;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 下单链路：加购物车 → 创建地址 → 下单 → 校验库存原子扣减与订单状态
 */
class OrderFlowApiTest extends BaseApiTest {

    @Test
    void createOrderDeductsStock() {
        String token = registerAndLogin(randomPhone(), "123456");
        resetGoods(TEST_GOODS_ID, 50);

        String orderNo = createOrder(token, TEST_GOODS_ID, 2);
        assertNotNull(orderNo);

        // 库存应原子扣减 2
        assertEquals(48, queryStock(TEST_GOODS_ID));
        // 订单应为待支付状态
        Integer orderStatus = jdbcTemplate.queryForObject(
                "select order_status from tb_novamall_order where order_no = ?", Integer.class, orderNo);
        assertEquals(0, orderStatus);
    }
}

package com.novamall.api;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 并发下单防超卖：库存 5 件，8 个用户同时下单各买 1 件，
 * 期望恰好 5 单成功、库存扣到 0 且不为负
 */
class ConcurrentOrderTest extends BaseApiTest {

    @Test
    void concurrentOrdersNeverOversell() throws InterruptedException {
        int stock = 5;
        int userCount = 8;
        resetGoods(TEST_GOODS_ID, stock);

        // 并发前顺序准备用户与下单参数，保证竞争只发生在下单扣库存环节
        List<String> tokens = new ArrayList<>();
        List<Map<String, Object>> orderParams = new ArrayList<>();
        for (int i = 0; i < userCount; i++) {
            String token = registerAndLogin(randomPhone(), "123456");
            tokens.add(token);
            orderParams.add(prepareOrderParam(token, TEST_GOODS_ID, 1));
        }

        ExecutorService executor = Executors.newFixedThreadPool(userCount);
        CountDownLatch startGate = new CountDownLatch(1);
        ConcurrentLinkedQueue<Integer> resultCodes = new ConcurrentLinkedQueue<>();
        for (int i = 0; i < userCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startGate.await();
                    Map<String, Object> result = postForMap("/api/v1/saveOrder", orderParams.get(index), tokens.get(index));
                    resultCodes.add((Integer) result.get("resultCode"));
                } catch (Exception e) {
                    resultCodes.add(-1);
                }
            });
        }
        // 统一放行，让 8 个请求尽量同时打到下单接口
        startGate.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(60, TimeUnit.SECONDS), "并发下单未在预期时间内完成");

        long successCount = resultCodes.stream().filter(code -> code == 200).count();
        assertEquals(userCount, resultCodes.size());
        assertEquals(stock, successCount, "库存 " + stock + " 件时应恰好 " + stock + " 单成功，实际响应码：" + resultCodes);
        assertEquals(0, queryStock(TEST_GOODS_ID), "库存应扣减到 0 且不为负数");
    }
}

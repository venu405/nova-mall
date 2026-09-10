package com.novamall.api.task;

import com.novamall.api.entity.NovaMallOrder;
import com.novamall.api.service.NovaMallOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;

/**
 * @apiNote 订单超时自动取消任务，每分钟扫描一次超时未支付的待支付订单，
 * 自动取消并回补库存；单批最多处理 100 笔，单笔失败不影响其他订单
 */
@Component
public class OrderTimeoutTask {

    private static final Logger logger = LoggerFactory.getLogger(OrderTimeoutTask.class);

    /** 单次扫描处理的最大订单数 */
    private static final int SCAN_LIMIT = 100;

    @Value("${novamall.order.pay-timeout-minutes:30}")
    private int payTimeoutMinutes;

    @Autowired
    private NovaMallOrderService novaMallOrderService;

    @Scheduled(fixedDelay = 60 * 1000, initialDelay = 60 * 1000)
    public void cancelTimeoutOrders() {
        Date expireTime = new Date(System.currentTimeMillis() - payTimeoutMinutes * 60 * 1000L);
        List<NovaMallOrder> timeoutOrders = novaMallOrderService.getTimeoutPrePayOrders(expireTime, SCAN_LIMIT);
        if (CollectionUtils.isEmpty(timeoutOrders)) {
            return;
        }
        logger.info("扫描到{}笔超时未支付订单（超时阈值{}分钟），开始自动取消", timeoutOrders.size(), payTimeoutMinutes);
        for (NovaMallOrder order : timeoutOrders) {
            try {
                Boolean result = novaMallOrderService.cancelOrderByTimeout(order.getOrderId());
                if (result) {
                    logger.info("订单[{}]超时未支付，已自动取消并回补库存", order.getOrderNo());
                }
            } catch (Exception e) {
                // 单笔订单处理失败不影响其他订单，等待下一轮扫描重试
                logger.error("订单[{}]超时取消失败，等待下一轮扫描重试", order.getOrderNo(), e);
            }
        }
    }
}

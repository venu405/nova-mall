/**
 * 严肃声明：
 * 开源版本请务必保留此注释头信息，若删除我方将保留所有法律责任追究！
 * 本软件已申请软件著作权，受国家版权局知识产权以及国家计算机软件著作权保护！
 * 可正常分享和学习源码，不得用于违法犯罪活动，违者必究！
 * Copyright (c) 2019-2021 十三 all rights reserved.
 * 版权所有，侵权必究！
 */
package com.novamall.api.service;

import com.novamall.api.api.mall.vo.NovaMallOrderDetailVO;
import com.novamall.api.api.mall.vo.NovaMallOrderItemVO;
import com.novamall.api.api.mall.vo.NovaMallShoppingCartItemVO;
import com.novamall.api.entity.MallUser;
import com.novamall.api.entity.MallUserAddress;
import com.novamall.api.entity.NovaMallOrder;
import com.novamall.api.util.PageQueryUtil;
import com.novamall.api.util.PageResult;

import java.util.List;

public interface NovaMallOrderService {
    /**
     * 获取订单详情
     *
     * @param orderId
     * @return
     */
    NovaMallOrderDetailVO getOrderDetailByOrderId(Long orderId);

    /**
     * 获取订单详情
     *
     * @param orderNo
     * @param userId
     * @return
     */
    NovaMallOrderDetailVO getOrderDetailByOrderNo(String orderNo, Long userId);

    /**
     * 我的订单列表
     *
     * @param pageUtil
     * @return
     */
    PageResult getMyOrders(PageQueryUtil pageUtil);

    /**
     * 手动取消订单
     *
     * @param orderNo
     * @param userId
     * @return
     */
    String cancelOrder(String orderNo, Long userId);

    /**
     * 确认收货
     *
     * @param orderNo
     * @param userId
     * @return
     */
    String finishOrder(String orderNo, Long userId);

    String paySuccess(String orderNo, int payType);

    String saveOrder(MallUser loginMallUser, MallUserAddress address, List<NovaMallShoppingCartItemVO> itemsForSave);

    /**
     * 后台分页
     *
     * @param pageUtil
     * @return
     */
    PageResult getNovaMallOrdersPage(PageQueryUtil pageUtil);

    /**
     * 订单信息修改
     *
     * @param novaMallOrder
     * @return
     */
    String updateOrderInfo(NovaMallOrder novaMallOrder);

    /**
     * 配货
     *
     * @param ids
     * @return
     */
    String checkDone(Long[] ids);

    /**
     * 出库
     *
     * @param ids
     * @return
     */
    String checkOut(Long[] ids);

    /**
     * 关闭订单
     *
     * @param ids
     * @return
     */
    String closeOrder(Long[] ids);

    List<NovaMallOrderItemVO> getOrderItems(Long orderId);
}

/**
 * 严肃声明：
 * 开源版本请务必保留此注释头信息，若删除我方将保留所有法律责任追究！
 * 本软件已申请软件著作权，受国家版权局知识产权以及国家计算机软件著作权保护！
 * 可正常分享和学习源码，不得用于违法犯罪活动，违者必究！
 * Copyright (c) 2019-2021 十三 all rights reserved.
 * 版权所有，侵权必究！
 */
package com.novamall.api.service.impl;

import com.novamall.api.api.mall.vo.NovaMallOrderDetailVO;
import com.novamall.api.api.mall.vo.NovaMallOrderItemVO;
import com.novamall.api.api.mall.vo.NovaMallOrderListVO;
import com.novamall.api.api.mall.vo.NovaMallShoppingCartItemVO;
import com.novamall.api.cache.CacheConstants;
import com.novamall.api.cache.NovaMallCacheService;
import com.novamall.api.common.*;
import com.novamall.api.dao.*;
import com.novamall.api.entity.*;
import com.novamall.api.service.NovaMallOrderService;
import com.novamall.api.util.BeanUtil;
import com.novamall.api.util.NumberUtil;
import com.novamall.api.util.PageQueryUtil;
import com.novamall.api.util.PageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

@Service
public class NovaMallOrderServiceImpl implements NovaMallOrderService {

    private static final Logger logger = LoggerFactory.getLogger(NovaMallOrderServiceImpl.class);

    @Autowired
    private NovaMallOrderMapper novaMallOrderMapper;
    @Autowired
    private NovaMallOrderItemMapper novaMallOrderItemMapper;
    @Autowired
    private NovaMallShoppingCartItemMapper novaMallShoppingCartItemMapper;
    @Autowired
    private NovaMallGoodsMapper novaMallGoodsMapper;
    @Autowired
    private NovaMallOrderAddressMapper novaMallOrderAddressMapper;
    @Autowired
    private NovaMallCacheService cacheService;

    @Override
    public NovaMallOrderDetailVO getOrderDetailByOrderId(Long orderId) {
        NovaMallOrder novaMallOrder = novaMallOrderMapper.selectByPrimaryKey(orderId);
        if (novaMallOrder == null) {
            NovaMallException.fail(ServiceResultEnum.DATA_NOT_EXIST.getResult());
        }
        List<NovaMallOrderItem> orderItems = novaMallOrderItemMapper.selectByOrderId(novaMallOrder.getOrderId());
        //获取订单项数据
        if (!CollectionUtils.isEmpty(orderItems)) {
            List<NovaMallOrderItemVO> novaMallOrderItemVOS = BeanUtil.copyList(orderItems, NovaMallOrderItemVO.class);
            NovaMallOrderDetailVO novaMallOrderDetailVO = new NovaMallOrderDetailVO();
            BeanUtil.copyProperties(novaMallOrder, novaMallOrderDetailVO);
            novaMallOrderDetailVO.setOrderStatusString(NovaMallOrderStatusEnum.getNovaMallOrderStatusEnumByStatus(novaMallOrderDetailVO.getOrderStatus()).getName());
            novaMallOrderDetailVO.setPayTypeString(PayTypeEnum.getPayTypeEnumByType(novaMallOrderDetailVO.getPayType()).getName());
            novaMallOrderDetailVO.setNovaMallOrderItemVOS(novaMallOrderItemVOS);
            return novaMallOrderDetailVO;
        } else {
            NovaMallException.fail(ServiceResultEnum.ORDER_ITEM_NULL_ERROR.getResult());
            return null;
        }
    }

    @Override
    public NovaMallOrderDetailVO getOrderDetailByOrderNo(String orderNo, Long userId) {
        NovaMallOrder novaMallOrder = novaMallOrderMapper.selectByOrderNo(orderNo);
        if (novaMallOrder == null) {
            NovaMallException.fail(ServiceResultEnum.DATA_NOT_EXIST.getResult());
        }
        if (!userId.equals(novaMallOrder.getUserId())) {
            NovaMallException.fail(ServiceResultEnum.REQUEST_FORBIDEN_ERROR.getResult());
        }
        List<NovaMallOrderItem> orderItems = novaMallOrderItemMapper.selectByOrderId(novaMallOrder.getOrderId());
        //获取订单项数据
        if (CollectionUtils.isEmpty(orderItems)) {
            NovaMallException.fail(ServiceResultEnum.ORDER_ITEM_NOT_EXIST_ERROR.getResult());
        }
        List<NovaMallOrderItemVO> novaMallOrderItemVOS = BeanUtil.copyList(orderItems, NovaMallOrderItemVO.class);
        NovaMallOrderDetailVO novaMallOrderDetailVO = new NovaMallOrderDetailVO();
        BeanUtil.copyProperties(novaMallOrder, novaMallOrderDetailVO);
        novaMallOrderDetailVO.setOrderStatusString(NovaMallOrderStatusEnum.getNovaMallOrderStatusEnumByStatus(novaMallOrderDetailVO.getOrderStatus()).getName());
        novaMallOrderDetailVO.setPayTypeString(PayTypeEnum.getPayTypeEnumByType(novaMallOrderDetailVO.getPayType()).getName());
        novaMallOrderDetailVO.setNovaMallOrderItemVOS(novaMallOrderItemVOS);
        return novaMallOrderDetailVO;
    }


    @Override
    public PageResult getMyOrders(PageQueryUtil pageUtil) {
        int total = novaMallOrderMapper.getTotalNovaMallOrders(pageUtil);
        List<NovaMallOrder> novaMallOrders = novaMallOrderMapper.findNovaMallOrderList(pageUtil);
        List<NovaMallOrderListVO> orderListVOS = new ArrayList<>();
        if (total > 0) {
            //数据转换 将实体类转成vo
            orderListVOS = BeanUtil.copyList(novaMallOrders, NovaMallOrderListVO.class);
            //设置订单状态中文显示值
            for (NovaMallOrderListVO novaMallOrderListVO : orderListVOS) {
                novaMallOrderListVO.setOrderStatusString(NovaMallOrderStatusEnum.getNovaMallOrderStatusEnumByStatus(novaMallOrderListVO.getOrderStatus()).getName());
            }
            List<Long> orderIds = novaMallOrders.stream().map(NovaMallOrder::getOrderId).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(orderIds)) {
                List<NovaMallOrderItem> orderItems = novaMallOrderItemMapper.selectByOrderIds(orderIds);
                Map<Long, List<NovaMallOrderItem>> itemByOrderIdMap = orderItems.stream().collect(groupingBy(NovaMallOrderItem::getOrderId));
                for (NovaMallOrderListVO novaMallOrderListVO : orderListVOS) {
                    //封装每个订单列表对象的订单项数据
                    if (itemByOrderIdMap.containsKey(novaMallOrderListVO.getOrderId())) {
                        List<NovaMallOrderItem> orderItemListTemp = itemByOrderIdMap.get(novaMallOrderListVO.getOrderId());
                        //将NovaMallOrderItem对象列表转换成NovaMallOrderItemVO对象列表
                        List<NovaMallOrderItemVO> novaMallOrderItemVOS = BeanUtil.copyList(orderItemListTemp, NovaMallOrderItemVO.class);
                        novaMallOrderListVO.setNovaMallOrderItemVOS(novaMallOrderItemVOS);
                    }
                }
            }
        }
        PageResult pageResult = new PageResult(orderListVOS, total, pageUtil.getLimit(), pageUtil.getPage());
        return pageResult;
    }

    @Override
    @Transactional
    public String cancelOrder(String orderNo, Long userId) {
        NovaMallOrder novaMallOrder = novaMallOrderMapper.selectByOrderNo(orderNo);
        if (novaMallOrder != null) {
            //验证是否是当前userId下的订单，否则报错
            if (!userId.equals(novaMallOrder.getUserId())) {
                NovaMallException.fail(ServiceResultEnum.NO_PERMISSION_ERROR.getResult());
            }
            //订单状态判断
            if (novaMallOrder.getOrderStatus().intValue() == NovaMallOrderStatusEnum.ORDER_SUCCESS.getOrderStatus()
                    || novaMallOrder.getOrderStatus().intValue() == NovaMallOrderStatusEnum.ORDER_CLOSED_BY_MALLUSER.getOrderStatus()
                    || novaMallOrder.getOrderStatus().intValue() == NovaMallOrderStatusEnum.ORDER_CLOSED_BY_EXPIRED.getOrderStatus()
                    || novaMallOrder.getOrderStatus().intValue() == NovaMallOrderStatusEnum.ORDER_CLOSED_BY_JUDGE.getOrderStatus()) {
                return ServiceResultEnum.ORDER_STATUS_ERROR.getResult();
            }
            //修改订单状态&&恢复库存
            if (novaMallOrderMapper.closeOrder(Collections.singletonList(novaMallOrder.getOrderId()), NovaMallOrderStatusEnum.ORDER_CLOSED_BY_MALLUSER.getOrderStatus()) > 0 && recoverStockNum(Collections.singletonList(novaMallOrder.getOrderId()))) {
                return ServiceResultEnum.SUCCESS.getResult();
            } else {
                return ServiceResultEnum.DB_ERROR.getResult();
            }
        }
        return ServiceResultEnum.ORDER_NOT_EXIST_ERROR.getResult();
    }

    @Override
    public String finishOrder(String orderNo, Long userId) {
        NovaMallOrder novaMallOrder = novaMallOrderMapper.selectByOrderNo(orderNo);
        if (novaMallOrder != null) {
            //验证是否是当前userId下的订单，否则报错
            if (!userId.equals(novaMallOrder.getUserId())) {
                return ServiceResultEnum.NO_PERMISSION_ERROR.getResult();
            }
            //订单状态判断 非出库状态下不进行修改操作
            if (novaMallOrder.getOrderStatus().intValue() != NovaMallOrderStatusEnum.ORDER_EXPRESS.getOrderStatus()) {
                return ServiceResultEnum.ORDER_STATUS_ERROR.getResult();
            }
            novaMallOrder.setOrderStatus((byte) NovaMallOrderStatusEnum.ORDER_SUCCESS.getOrderStatus());
            novaMallOrder.setUpdateTime(new Date());
            if (novaMallOrderMapper.updateByPrimaryKeySelective(novaMallOrder) > 0) {
                return ServiceResultEnum.SUCCESS.getResult();
            } else {
                return ServiceResultEnum.DB_ERROR.getResult();
            }
        }
        return ServiceResultEnum.ORDER_NOT_EXIST_ERROR.getResult();
    }

    @Override
    public String paySuccess(String orderNo, int payType) {
        NovaMallOrder novaMallOrder = novaMallOrderMapper.selectByOrderNo(orderNo);
        if (novaMallOrder != null) {
            //订单状态判断 非待支付状态下不进行修改操作
            if (novaMallOrder.getOrderStatus().intValue() != NovaMallOrderStatusEnum.ORDER_PRE_PAY.getOrderStatus()) {
                return ServiceResultEnum.ORDER_STATUS_ERROR.getResult();
            }
            novaMallOrder.setOrderStatus((byte) NovaMallOrderStatusEnum.ORDER_PAID.getOrderStatus());
            novaMallOrder.setPayType((byte) payType);
            novaMallOrder.setPayStatus((byte) PayStatusEnum.PAY_SUCCESS.getPayStatus());
            novaMallOrder.setPayTime(new Date());
            novaMallOrder.setUpdateTime(new Date());
            if (novaMallOrderMapper.updateByPrimaryKeySelective(novaMallOrder) > 0) {
                return ServiceResultEnum.SUCCESS.getResult();
            } else {
                return ServiceResultEnum.DB_ERROR.getResult();
            }
        }
        return ServiceResultEnum.ORDER_NOT_EXIST_ERROR.getResult();
    }

    @Override
    @Transactional
    public String saveOrder(MallUser loginMallUser, MallUserAddress address, List<NovaMallShoppingCartItemVO> myShoppingCartItems) {
        List<Long> itemIdList = myShoppingCartItems.stream().map(NovaMallShoppingCartItemVO::getCartItemId).collect(Collectors.toList());
        List<Long> goodsIds = myShoppingCartItems.stream().map(NovaMallShoppingCartItemVO::getGoodsId).collect(Collectors.toList());
        List<NovaMallGoods> novaMallGoods = novaMallGoodsMapper.selectByPrimaryKeys(goodsIds);
        //检查是否包含已下架商品
        List<NovaMallGoods> goodsListNotSelling = novaMallGoods.stream()
                .filter(novaMallGoodsTemp -> novaMallGoodsTemp.getGoodsSellStatus() != Constants.SELL_STATUS_UP)
                .collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(goodsListNotSelling)) {
            //goodsListNotSelling 对象非空则表示有下架商品
            NovaMallException.fail(goodsListNotSelling.get(0).getGoodsName() + "已下架，无法生成订单");
        }
        Map<Long, NovaMallGoods> novaMallGoodsMap = novaMallGoods.stream().collect(Collectors.toMap(NovaMallGoods::getGoodsId, Function.identity(), (entity1, entity2) -> entity1));
        //判断商品库存
        for (NovaMallShoppingCartItemVO shoppingCartItemVO : myShoppingCartItems) {
            //查出的商品中不存在购物车中的这条关联商品数据，直接返回错误提醒
            if (!novaMallGoodsMap.containsKey(shoppingCartItemVO.getGoodsId())) {
                NovaMallException.fail(ServiceResultEnum.SHOPPING_ITEM_ERROR.getResult());
            }
            //存在数量大于库存的情况，直接返回错误提醒
            if (shoppingCartItemVO.getGoodsCount() > novaMallGoodsMap.get(shoppingCartItemVO.getGoodsId()).getStockNum()) {
                NovaMallException.fail(ServiceResultEnum.SHOPPING_ITEM_COUNT_ERROR.getResult());
            }
        }
        //删除购物项
        if (!CollectionUtils.isEmpty(itemIdList) && !CollectionUtils.isEmpty(goodsIds) && !CollectionUtils.isEmpty(novaMallGoods)) {
            if (novaMallShoppingCartItemMapper.deleteBatch(itemIdList) > 0) {
                List<StockNumDTO> stockNumDTOS = BeanUtil.copyList(myShoppingCartItems, StockNumDTO.class);
                int updateStockNumResult = novaMallGoodsMapper.updateStockNum(stockNumDTOS);
                if (updateStockNumResult < 1) {
                    NovaMallException.fail(ServiceResultEnum.SHOPPING_ITEM_COUNT_ERROR.getResult());
                }
                //生成订单号
                String orderNo = NumberUtil.genOrderNo();
                int priceTotal = 0;
                //保存订单
                NovaMallOrder novaMallOrder = new NovaMallOrder();
                novaMallOrder.setOrderNo(orderNo);
                novaMallOrder.setUserId(loginMallUser.getUserId());
                //总价
                for (NovaMallShoppingCartItemVO novaMallShoppingCartItemVO : myShoppingCartItems) {
                    priceTotal += novaMallShoppingCartItemVO.getGoodsCount() * novaMallShoppingCartItemVO.getSellingPrice();
                }
                if (priceTotal < 1) {
                    NovaMallException.fail(ServiceResultEnum.ORDER_PRICE_ERROR.getResult());
                }
                novaMallOrder.setTotalPrice(priceTotal);
                String extraInfo = "";
                novaMallOrder.setExtraInfo(extraInfo);
                //生成订单项并保存订单项纪录
                if (novaMallOrderMapper.insertSelective(novaMallOrder) > 0) {
                    //生成订单收货地址快照，并保存至数据库
                    NovaMallOrderAddress novaMallOrderAddress = new NovaMallOrderAddress();
                    BeanUtil.copyProperties(address, novaMallOrderAddress);
                    novaMallOrderAddress.setOrderId(novaMallOrder.getOrderId());
                    //生成所有的订单项快照，并保存至数据库
                    List<NovaMallOrderItem> novaMallOrderItems = new ArrayList<>();
                    for (NovaMallShoppingCartItemVO novaMallShoppingCartItemVO : myShoppingCartItems) {
                        NovaMallOrderItem novaMallOrderItem = new NovaMallOrderItem();
                        //使用BeanUtil工具类将novaMallShoppingCartItemVO中的属性复制到novaMallOrderItem对象中
                        BeanUtil.copyProperties(novaMallShoppingCartItemVO, novaMallOrderItem);
                        //NovaMallOrderMapper文件insert()方法中使用了useGeneratedKeys因此orderId可以获取到
                        novaMallOrderItem.setOrderId(novaMallOrder.getOrderId());
                        novaMallOrderItems.add(novaMallOrderItem);
                    }
                    //保存至数据库
                    if (novaMallOrderItemMapper.insertBatch(novaMallOrderItems) > 0 && novaMallOrderAddressMapper.insertSelective(novaMallOrderAddress) > 0) {
                        //所有操作成功后，将订单号返回，以供Controller方法跳转到订单详情
                        return orderNo;
                    }
                    NovaMallException.fail(ServiceResultEnum.ORDER_PRICE_ERROR.getResult());
                }
                NovaMallException.fail(ServiceResultEnum.DB_ERROR.getResult());
            }
            NovaMallException.fail(ServiceResultEnum.DB_ERROR.getResult());
        }
        NovaMallException.fail(ServiceResultEnum.SHOPPING_ITEM_ERROR.getResult());
        return ServiceResultEnum.SHOPPING_ITEM_ERROR.getResult();
    }


    @Override
    public List<NovaMallOrder> getTimeoutPrePayOrders(Date expireTime, int limit) {
        return novaMallOrderMapper.selectTimeoutPrePayOrders(expireTime, limit);
    }

    @Override
    @Transactional
    public Boolean cancelOrderByTimeout(Long orderId) {
        // 乐观条件更新：WHERE 中带 order_status=待支付，与支付成功/手动关闭并发时只有一个能生效
        int closeResult = novaMallOrderMapper.closeOrderIfPrePay(orderId, NovaMallOrderStatusEnum.ORDER_CLOSED_BY_EXPIRED.getOrderStatus());
        if (closeResult <= 0) {
            // 影响行数为 0，订单已被支付或关闭，跳过不处理
            return false;
        }
        // 关闭成功后回补库存，取消+回补在同一事务内
        List<NovaMallOrderItem> orderItems = novaMallOrderItemMapper.selectByOrderId(orderId);
        if (!CollectionUtils.isEmpty(orderItems)) {
            List<StockNumDTO> stockNumDTOS = BeanUtil.copyList(orderItems, StockNumDTO.class);
            int addBackResult = novaMallGoodsMapper.addBackStockNum(stockNumDTOS);
            if (addBackResult < 1) {
                logger.error("订单超时取消回补库存失败，orderId={}", orderId);
                NovaMallException.fail(ServiceResultEnum.DB_ERROR.getResult());
            }
            for (NovaMallOrderItem orderItem : orderItems) {
                logger.info("订单超时取消回补库存，orderId={}，goodsId={}，数量={}", orderId, orderItem.getGoodsId(), orderItem.getGoodsCount());
                // 库存已变化，删除商品详情缓存使前台立即拿到最新库存（首页配置 VO 不含库存字段，无需处理）
                cacheService.delete(CacheConstants.GOODS_DETAIL_KEY + orderItem.getGoodsId());
            }
        }
        return true;
    }

    @Override
    public PageResult getNovaMallOrdersPage(PageQueryUtil pageUtil) {
        List<NovaMallOrder> novaMallOrders = novaMallOrderMapper.findNovaMallOrderList(pageUtil);
        int total = novaMallOrderMapper.getTotalNovaMallOrders(pageUtil);
        PageResult pageResult = new PageResult(novaMallOrders, total, pageUtil.getLimit(), pageUtil.getPage());
        return pageResult;
    }

    @Override
    @Transactional
    public String updateOrderInfo(NovaMallOrder novaMallOrder) {
        NovaMallOrder temp = novaMallOrderMapper.selectByPrimaryKey(novaMallOrder.getOrderId());
        //不为空且orderStatus>=0且状态为出库之前可以修改部分信息
        if (temp != null && temp.getOrderStatus() >= 0 && temp.getOrderStatus() < 3) {
            temp.setTotalPrice(novaMallOrder.getTotalPrice());
            temp.setUpdateTime(new Date());
            if (novaMallOrderMapper.updateByPrimaryKeySelective(temp) > 0) {
                return ServiceResultEnum.SUCCESS.getResult();
            }
            return ServiceResultEnum.DB_ERROR.getResult();
        }
        return ServiceResultEnum.DATA_NOT_EXIST.getResult();
    }

    @Override
    @Transactional
    public String checkDone(Long[] ids) {
        //查询所有的订单 判断状态 修改状态和更新时间
        List<NovaMallOrder> orders = novaMallOrderMapper.selectByPrimaryKeys(Arrays.asList(ids));
        String errorOrderNos = "";
        if (!CollectionUtils.isEmpty(orders)) {
            for (NovaMallOrder novaMallOrder : orders) {
                if (novaMallOrder.getIsDeleted() == 1) {
                    errorOrderNos += novaMallOrder.getOrderNo() + " ";
                    continue;
                }
                if (novaMallOrder.getOrderStatus() != 1) {
                    errorOrderNos += novaMallOrder.getOrderNo() + " ";
                }
            }
            if (!StringUtils.hasText(errorOrderNos)) {
                //订单状态正常 可以执行配货完成操作 修改订单状态和更新时间
                if (novaMallOrderMapper.checkDone(Arrays.asList(ids)) > 0) {
                    return ServiceResultEnum.SUCCESS.getResult();
                } else {
                    return ServiceResultEnum.DB_ERROR.getResult();
                }
            } else {
                //订单此时不可执行出库操作
                if (errorOrderNos.length() > 0 && errorOrderNos.length() < 100) {
                    return errorOrderNos + "订单的状态不是支付成功无法执行出库操作";
                } else {
                    return "你选择了太多状态不是支付成功的订单，无法执行配货完成操作";
                }
            }
        }
        //未查询到数据 返回错误提示
        return ServiceResultEnum.DATA_NOT_EXIST.getResult();
    }

    @Override
    @Transactional
    public String checkOut(Long[] ids) {
        //查询所有的订单 判断状态 修改状态和更新时间
        List<NovaMallOrder> orders = novaMallOrderMapper.selectByPrimaryKeys(Arrays.asList(ids));
        String errorOrderNos = "";
        if (!CollectionUtils.isEmpty(orders)) {
            for (NovaMallOrder novaMallOrder : orders) {
                if (novaMallOrder.getIsDeleted() == 1) {
                    errorOrderNos += novaMallOrder.getOrderNo() + " ";
                    continue;
                }
                if (novaMallOrder.getOrderStatus() != 1 && novaMallOrder.getOrderStatus() != 2) {
                    errorOrderNos += novaMallOrder.getOrderNo() + " ";
                }
            }
            if (!StringUtils.hasText(errorOrderNos)) {
                //订单状态正常 可以执行出库操作 修改订单状态和更新时间
                if (novaMallOrderMapper.checkOut(Arrays.asList(ids)) > 0) {
                    return ServiceResultEnum.SUCCESS.getResult();
                } else {
                    return ServiceResultEnum.DB_ERROR.getResult();
                }
            } else {
                //订单此时不可执行出库操作
                if (errorOrderNos.length() > 0 && errorOrderNos.length() < 100) {
                    return errorOrderNos + "订单的状态不是支付成功或配货完成无法执行出库操作";
                } else {
                    return "你选择了太多状态不是支付成功或配货完成的订单，无法执行出库操作";
                }
            }
        }
        //未查询到数据 返回错误提示
        return ServiceResultEnum.DATA_NOT_EXIST.getResult();
    }

    @Override
    @Transactional
    public String closeOrder(Long[] ids) {
        //查询所有的订单 判断状态 修改状态和更新时间
        List<NovaMallOrder> orders = novaMallOrderMapper.selectByPrimaryKeys(Arrays.asList(ids));
        String errorOrderNos = "";
        if (!CollectionUtils.isEmpty(orders)) {
            for (NovaMallOrder novaMallOrder : orders) {
                // isDeleted=1 一定为已关闭订单
                if (novaMallOrder.getIsDeleted() == 1) {
                    errorOrderNos += novaMallOrder.getOrderNo() + " ";
                    continue;
                }
                //已关闭或者已完成无法关闭订单
                if (novaMallOrder.getOrderStatus() == 4 || novaMallOrder.getOrderStatus() < 0) {
                    errorOrderNos += novaMallOrder.getOrderNo() + " ";
                }
            }
            if (!StringUtils.hasText(errorOrderNos)) {
                //订单状态正常 可以执行关闭操作 修改订单状态和更新时间&&恢复库存
                if (novaMallOrderMapper.closeOrder(Arrays.asList(ids), NovaMallOrderStatusEnum.ORDER_CLOSED_BY_JUDGE.getOrderStatus()) > 0 && recoverStockNum(Arrays.asList(ids))) {
                    return ServiceResultEnum.SUCCESS.getResult();
                } else {
                    return ServiceResultEnum.DB_ERROR.getResult();
                }
            } else {
                //订单此时不可执行关闭操作
                if (errorOrderNos.length() > 0 && errorOrderNos.length() < 100) {
                    return errorOrderNos + "订单不能执行关闭操作";
                } else {
                    return "你选择的订单不能执行关闭操作";
                }
            }
        }
        //未查询到数据 返回错误提示
        return ServiceResultEnum.DATA_NOT_EXIST.getResult();
    }

    @Override
    public List<NovaMallOrderItemVO> getOrderItems(Long orderId) {
        NovaMallOrder novaMallOrder = novaMallOrderMapper.selectByPrimaryKey(orderId);
        if (novaMallOrder != null) {
            List<NovaMallOrderItem> orderItems = novaMallOrderItemMapper.selectByOrderId(novaMallOrder.getOrderId());
            //获取订单项数据
            if (!CollectionUtils.isEmpty(orderItems)) {
                List<NovaMallOrderItemVO> novaMallOrderItemVOS = BeanUtil.copyList(orderItems, NovaMallOrderItemVO.class);
                return novaMallOrderItemVOS;
            }
        }
        return null;
    }

    /**
     * 恢复库存
     *
     * @param orderIds
     * @return
     */
    public Boolean recoverStockNum(List<Long> orderIds) {
        //查询对应的订单项
        List<NovaMallOrderItem> novaMallOrderItems = novaMallOrderItemMapper.selectByOrderIds(orderIds);
        //获取对应的商品id和商品数量并赋值到StockNumDTO对象中
        List<StockNumDTO> stockNumDTOS = BeanUtil.copyList(novaMallOrderItems, StockNumDTO.class);
        //执行恢复库存的操作
        int updateStockNumResult = novaMallGoodsMapper.recoverStockNum(stockNumDTOS);
        if (updateStockNumResult < 1) {
            NovaMallException.fail(ServiceResultEnum.CLOSE_ORDER_ERROR.getResult());
            return false;
        } else {
            return true;
        }
    }
}

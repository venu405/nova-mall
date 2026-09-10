/**
 * 严肃声明：
 * 开源版本请务必保留此注释头信息，若删除我方将保留所有法律责任追究！
 * 本软件已申请软件著作权，受国家版权局知识产权以及国家计算机软件著作权保护！
 * 可正常分享和学习源码，不得用于违法犯罪活动，违者必究！
 * Copyright (c) 2019-2021 十三 all rights reserved.
 * 版权所有，侵权必究！
 */
package com.novamall.api.dao;

import com.novamall.api.entity.NovaMallOrder;
import com.novamall.api.util.PageQueryUtil;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface NovaMallOrderMapper {
    int deleteByPrimaryKey(Long orderId);

    int insert(NovaMallOrder record);

    int insertSelective(NovaMallOrder record);

    NovaMallOrder selectByPrimaryKey(Long orderId);

    NovaMallOrder selectByOrderNo(String orderNo);

    int updateByPrimaryKeySelective(NovaMallOrder record);

    int updateByPrimaryKey(NovaMallOrder record);

    List<NovaMallOrder> findNovaMallOrderList(PageQueryUtil pageUtil);

    int getTotalNovaMallOrders(PageQueryUtil pageUtil);

    List<NovaMallOrder> selectByPrimaryKeys(@Param("orderIds") List<Long> orderIds);

    int checkOut(@Param("orderIds") List<Long> orderIds);

    int closeOrder(@Param("orderIds") List<Long> orderIds, @Param("orderStatus") int orderStatus);

    int checkDone(@Param("orderIds") List<Long> asList);

    List<NovaMallOrder> selectTimeoutPrePayOrders(@Param("expireTime") Date expireTime, @Param("limit") int limit);

    int closeOrderIfPrePay(@Param("orderId") Long orderId, @Param("orderStatus") int orderStatus);
}
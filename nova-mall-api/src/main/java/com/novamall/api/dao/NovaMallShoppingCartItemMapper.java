/**
 * 严肃声明：
 * 开源版本请务必保留此注释头信息，若删除我方将保留所有法律责任追究！
 * 本软件已申请软件著作权，受国家版权局知识产权以及国家计算机软件著作权保护！
 * 可正常分享和学习源码，不得用于违法犯罪活动，违者必究！
 * Copyright (c) 2019-2021 十三 all rights reserved.
 * 版权所有，侵权必究！
 */
package com.novamall.api.dao;

import com.novamall.api.entity.NovaMallShoppingCartItem;
import com.novamall.api.util.PageQueryUtil;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface NovaMallShoppingCartItemMapper {
    int deleteByPrimaryKey(Long cartItemId);

    int insert(NovaMallShoppingCartItem record);

    int insertSelective(NovaMallShoppingCartItem record);

    NovaMallShoppingCartItem selectByPrimaryKey(Long cartItemId);

    NovaMallShoppingCartItem selectByUserIdAndGoodsId(@Param("novaMallUserId") Long novaMallUserId, @Param("goodsId") Long goodsId);

    List<NovaMallShoppingCartItem> selectByUserId(@Param("novaMallUserId") Long novaMallUserId, @Param("number") int number);

    List<NovaMallShoppingCartItem> selectByUserIdAndCartItemIds(@Param("novaMallUserId") Long novaMallUserId, @Param("cartItemIds") List<Long> cartItemIds);

    int selectCountByUserId(Long novaMallUserId);

    int updateByPrimaryKeySelective(NovaMallShoppingCartItem record);

    int updateByPrimaryKey(NovaMallShoppingCartItem record);

    int deleteBatch(List<Long> ids);

    List<NovaMallShoppingCartItem> findMyNovaMallCartItems(PageQueryUtil pageUtil);

    int getTotalMyNovaMallCartItems(PageQueryUtil pageUtil);
}
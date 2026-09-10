/**
 * 严肃声明：
 * 开源版本请务必保留此注释头信息，若删除我方将保留所有法律责任追究！
 * 本软件已申请软件著作权，受国家版权局知识产权以及国家计算机软件著作权保护！
 * 可正常分享和学习源码，不得用于违法犯罪活动，违者必究！
 * Copyright (c) 2019-2021 十三 all rights reserved.
 * 版权所有，侵权必究！
 */
package com.novamall.api.service;

import com.novamall.api.api.mall.param.SaveCartItemParam;
import com.novamall.api.api.mall.param.UpdateCartItemParam;
import com.novamall.api.api.mall.vo.NovaMallShoppingCartItemVO;
import com.novamall.api.entity.NovaMallShoppingCartItem;
import com.novamall.api.util.PageQueryUtil;
import com.novamall.api.util.PageResult;

import java.util.List;

public interface NovaMallShoppingCartService {

    /**
     * 保存商品至购物车中
     *
     * @param saveCartItemParam
     * @param userId
     * @return
     */
    String saveNovaMallCartItem(SaveCartItemParam saveCartItemParam, Long userId);

    /**
     * 修改购物车中的属性
     *
     * @param updateCartItemParam
     * @param userId
     * @return
     */
    String updateNovaMallCartItem(UpdateCartItemParam updateCartItemParam, Long userId);

    /**
     * 获取购物项详情
     *
     * @param novaMallShoppingCartItemId
     * @return
     */
    NovaMallShoppingCartItem getNovaMallCartItemById(Long novaMallShoppingCartItemId);

    /**
     * 删除购物车中的商品
     *
     *
     * @param shoppingCartItemId
     * @param userId
     * @return
     */
    Boolean deleteById(Long shoppingCartItemId, Long userId);

    /**
     * 获取我的购物车中的列表数据
     *
     * @param novaMallUserId
     * @return
     */
    List<NovaMallShoppingCartItemVO> getMyShoppingCartItems(Long novaMallUserId);

    /**
     * 根据userId和cartItemIds获取对应的购物项记录
     *
     * @param cartItemIds
     * @param novaMallUserId
     * @return
     */
    List<NovaMallShoppingCartItemVO> getCartItemsForSettle(List<Long> cartItemIds, Long novaMallUserId);

    /**
     * 我的购物车(分页数据)
     *
     * @param pageUtil
     * @return
     */
    PageResult getMyShoppingCartItems(PageQueryUtil pageUtil);
}

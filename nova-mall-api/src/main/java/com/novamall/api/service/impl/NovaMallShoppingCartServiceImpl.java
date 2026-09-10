/**
 * 严肃声明：
 * 开源版本请务必保留此注释头信息，若删除我方将保留所有法律责任追究！
 * 本软件已申请软件著作权，受国家版权局知识产权以及国家计算机软件著作权保护！
 * 可正常分享和学习源码，不得用于违法犯罪活动，违者必究！
 * Copyright (c) 2019-2021 十三 all rights reserved.
 * 版权所有，侵权必究！
 */
package com.novamall.api.service.impl;

import com.novamall.api.api.mall.param.SaveCartItemParam;
import com.novamall.api.api.mall.param.UpdateCartItemParam;
import com.novamall.api.common.Constants;
import com.novamall.api.common.NovaMallException;
import com.novamall.api.common.ServiceResultEnum;
import com.novamall.api.api.mall.vo.NovaMallShoppingCartItemVO;
import com.novamall.api.dao.NovaMallGoodsMapper;
import com.novamall.api.dao.NovaMallShoppingCartItemMapper;
import com.novamall.api.entity.NovaMallGoods;
import com.novamall.api.entity.NovaMallShoppingCartItem;
import com.novamall.api.service.NovaMallShoppingCartService;
import com.novamall.api.util.BeanUtil;
import com.novamall.api.util.PageQueryUtil;
import com.novamall.api.util.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class NovaMallShoppingCartServiceImpl implements NovaMallShoppingCartService {

    @Autowired
    private NovaMallShoppingCartItemMapper novaMallShoppingCartItemMapper;

    @Autowired
    private NovaMallGoodsMapper novaMallGoodsMapper;

    @Override
    public String saveNovaMallCartItem(SaveCartItemParam saveCartItemParam, Long userId) {
        NovaMallShoppingCartItem temp = novaMallShoppingCartItemMapper.selectByUserIdAndGoodsId(userId, saveCartItemParam.getGoodsId());
        if (temp != null) {
            //已存在则修改该记录
            NovaMallException.fail(ServiceResultEnum.SHOPPING_CART_ITEM_EXIST_ERROR.getResult());
        }
        NovaMallGoods novaMallGoods = novaMallGoodsMapper.selectByPrimaryKey(saveCartItemParam.getGoodsId());
        //商品为空
        if (novaMallGoods == null) {
            return ServiceResultEnum.GOODS_NOT_EXIST.getResult();
        }
        int totalItem = novaMallShoppingCartItemMapper.selectCountByUserId(userId);
        //超出单个商品的最大数量
        if (saveCartItemParam.getGoodsCount() < 1) {
            return ServiceResultEnum.SHOPPING_CART_ITEM_NUMBER_ERROR.getResult();
        }
        //超出单个商品的最大数量
        if (saveCartItemParam.getGoodsCount() > Constants.SHOPPING_CART_ITEM_LIMIT_NUMBER) {
            return ServiceResultEnum.SHOPPING_CART_ITEM_LIMIT_NUMBER_ERROR.getResult();
        }
        //超出最大数量
        if (totalItem > Constants.SHOPPING_CART_ITEM_TOTAL_NUMBER) {
            return ServiceResultEnum.SHOPPING_CART_ITEM_TOTAL_NUMBER_ERROR.getResult();
        }
        NovaMallShoppingCartItem novaMallShoppingCartItem = new NovaMallShoppingCartItem();
        BeanUtil.copyProperties(saveCartItemParam, novaMallShoppingCartItem);
        novaMallShoppingCartItem.setUserId(userId);
        //保存记录
        if (novaMallShoppingCartItemMapper.insertSelective(novaMallShoppingCartItem) > 0) {
            return ServiceResultEnum.SUCCESS.getResult();
        }
        return ServiceResultEnum.DB_ERROR.getResult();
    }

    @Override
    public String updateNovaMallCartItem(UpdateCartItemParam updateCartItemParam, Long userId) {
        NovaMallShoppingCartItem novaMallShoppingCartItemUpdate = novaMallShoppingCartItemMapper.selectByPrimaryKey(updateCartItemParam.getCartItemId());
        if (novaMallShoppingCartItemUpdate == null) {
            return ServiceResultEnum.DATA_NOT_EXIST.getResult();
        }
        if (!novaMallShoppingCartItemUpdate.getUserId().equals(userId)) {
            NovaMallException.fail(ServiceResultEnum.REQUEST_FORBIDEN_ERROR.getResult());
        }
        //超出单个商品的最大数量
        if (updateCartItemParam.getGoodsCount() > Constants.SHOPPING_CART_ITEM_LIMIT_NUMBER) {
            return ServiceResultEnum.SHOPPING_CART_ITEM_LIMIT_NUMBER_ERROR.getResult();
        }
        //当前登录账号的userId与待修改的cartItem中userId不同，返回错误
        if (!novaMallShoppingCartItemUpdate.getUserId().equals(userId)) {
            return ServiceResultEnum.NO_PERMISSION_ERROR.getResult();
        }
        //数值相同，则不执行数据操作
        if (updateCartItemParam.getGoodsCount().equals(novaMallShoppingCartItemUpdate.getGoodsCount())) {
            return ServiceResultEnum.SUCCESS.getResult();
        }
        novaMallShoppingCartItemUpdate.setGoodsCount(updateCartItemParam.getGoodsCount());
        novaMallShoppingCartItemUpdate.setUpdateTime(new Date());
        //修改记录
        if (novaMallShoppingCartItemMapper.updateByPrimaryKeySelective(novaMallShoppingCartItemUpdate) > 0) {
            return ServiceResultEnum.SUCCESS.getResult();
        }
        return ServiceResultEnum.DB_ERROR.getResult();
    }

    @Override
    public NovaMallShoppingCartItem getNovaMallCartItemById(Long novaMallShoppingCartItemId) {
        NovaMallShoppingCartItem novaMallShoppingCartItem = novaMallShoppingCartItemMapper.selectByPrimaryKey(novaMallShoppingCartItemId);
        if (novaMallShoppingCartItem == null) {
            NovaMallException.fail(ServiceResultEnum.DATA_NOT_EXIST.getResult());
        }
        return novaMallShoppingCartItem;
    }

    @Override
    public Boolean deleteById(Long shoppingCartItemId, Long userId) {
        NovaMallShoppingCartItem novaMallShoppingCartItem = novaMallShoppingCartItemMapper.selectByPrimaryKey(shoppingCartItemId);
        if (novaMallShoppingCartItem == null) {
            return false;
        }
        //userId不同不能删除
        if (!userId.equals(novaMallShoppingCartItem.getUserId())) {
            return false;
        }
        return novaMallShoppingCartItemMapper.deleteByPrimaryKey(shoppingCartItemId) > 0;
    }

    @Override
    public List<NovaMallShoppingCartItemVO> getMyShoppingCartItems(Long novaMallUserId) {
        List<NovaMallShoppingCartItemVO> novaMallShoppingCartItemVOS = new ArrayList<>();
        List<NovaMallShoppingCartItem> novaMallShoppingCartItems = novaMallShoppingCartItemMapper.selectByUserId(novaMallUserId, Constants.SHOPPING_CART_ITEM_TOTAL_NUMBER);
        return getNovaMallShoppingCartItemVOS(novaMallShoppingCartItemVOS, novaMallShoppingCartItems);
    }

    @Override
    public List<NovaMallShoppingCartItemVO> getCartItemsForSettle(List<Long> cartItemIds, Long novaMallUserId) {
        List<NovaMallShoppingCartItemVO> novaMallShoppingCartItemVOS = new ArrayList<>();
        if (CollectionUtils.isEmpty(cartItemIds)) {
            NovaMallException.fail("购物项不能为空");
        }
        List<NovaMallShoppingCartItem> novaMallShoppingCartItems = novaMallShoppingCartItemMapper.selectByUserIdAndCartItemIds(novaMallUserId, cartItemIds);
        if (CollectionUtils.isEmpty(novaMallShoppingCartItems)) {
            NovaMallException.fail("购物项不能为空");
        }
        if (novaMallShoppingCartItems.size() != cartItemIds.size()) {
            NovaMallException.fail("参数异常");
        }
        return getNovaMallShoppingCartItemVOS(novaMallShoppingCartItemVOS, novaMallShoppingCartItems);
    }

    /**
     * 数据转换
     *
     * @param novaMallShoppingCartItemVOS
     * @param novaMallShoppingCartItems
     * @return
     */
    private List<NovaMallShoppingCartItemVO> getNovaMallShoppingCartItemVOS(List<NovaMallShoppingCartItemVO> novaMallShoppingCartItemVOS, List<NovaMallShoppingCartItem> novaMallShoppingCartItems) {
        if (!CollectionUtils.isEmpty(novaMallShoppingCartItems)) {
            //查询商品信息并做数据转换
            List<Long> novaMallGoodsIds = novaMallShoppingCartItems.stream().map(NovaMallShoppingCartItem::getGoodsId).collect(Collectors.toList());
            List<NovaMallGoods> novaMallGoods = novaMallGoodsMapper.selectByPrimaryKeys(novaMallGoodsIds);
            Map<Long, NovaMallGoods> novaMallGoodsMap = new HashMap<>();
            if (!CollectionUtils.isEmpty(novaMallGoods)) {
                novaMallGoodsMap = novaMallGoods.stream().collect(Collectors.toMap(NovaMallGoods::getGoodsId, Function.identity(), (entity1, entity2) -> entity1));
            }
            for (NovaMallShoppingCartItem novaMallShoppingCartItem : novaMallShoppingCartItems) {
                NovaMallShoppingCartItemVO novaMallShoppingCartItemVO = new NovaMallShoppingCartItemVO();
                BeanUtil.copyProperties(novaMallShoppingCartItem, novaMallShoppingCartItemVO);
                if (novaMallGoodsMap.containsKey(novaMallShoppingCartItem.getGoodsId())) {
                    NovaMallGoods novaMallGoodsTemp = novaMallGoodsMap.get(novaMallShoppingCartItem.getGoodsId());
                    novaMallShoppingCartItemVO.setGoodsCoverImg(novaMallGoodsTemp.getGoodsCoverImg());
                    String goodsName = novaMallGoodsTemp.getGoodsName();
                    // 字符串过长导致文字超出的问题
                    if (goodsName.length() > 28) {
                        goodsName = goodsName.substring(0, 28) + "...";
                    }
                    novaMallShoppingCartItemVO.setGoodsName(goodsName);
                    novaMallShoppingCartItemVO.setSellingPrice(novaMallGoodsTemp.getSellingPrice());
                    novaMallShoppingCartItemVOS.add(novaMallShoppingCartItemVO);
                }
            }
        }
        return novaMallShoppingCartItemVOS;
    }

    @Override
    public PageResult getMyShoppingCartItems(PageQueryUtil pageUtil) {
        List<NovaMallShoppingCartItemVO> novaMallShoppingCartItemVOS = new ArrayList<>();
        List<NovaMallShoppingCartItem> novaMallShoppingCartItems = novaMallShoppingCartItemMapper.findMyNovaMallCartItems(pageUtil);
        int total = novaMallShoppingCartItemMapper.getTotalMyNovaMallCartItems(pageUtil);
        PageResult pageResult = new PageResult(getNovaMallShoppingCartItemVOS(novaMallShoppingCartItemVOS, novaMallShoppingCartItems), total, pageUtil.getLimit(), pageUtil.getPage());
        return pageResult;
    }
}

/**
 * 严肃声明：
 * 开源版本请务必保留此注释头信息，若删除我方将保留所有法律责任追究！
 * 本软件已申请软件著作权，受国家版权局知识产权以及国家计算机软件著作权保护！
 * 可正常分享和学习源码，不得用于违法犯罪活动，违者必究！
 * Copyright (c) 2019-2021 十三 all rights reserved.
 * 版权所有，侵权必究！
 */
package com.novamall.api.api.mall;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import com.novamall.api.api.mall.param.SaveCartItemParam;
import com.novamall.api.api.mall.param.UpdateCartItemParam;
import com.novamall.api.common.Constants;
import com.novamall.api.common.NovaMallException;
import com.novamall.api.common.ServiceResultEnum;
import com.novamall.api.config.annotation.TokenToMallUser;
import com.novamall.api.api.mall.vo.NovaMallShoppingCartItemVO;
import com.novamall.api.entity.MallUser;
import com.novamall.api.entity.NovaMallShoppingCartItem;
import com.novamall.api.service.NovaMallShoppingCartService;
import com.novamall.api.util.PageQueryUtil;
import com.novamall.api.util.PageResult;
import com.novamall.api.util.Result;
import com.novamall.api.util.ResultGenerator;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Api(value = "v1", tags = "5.NovaMall商城购物车相关接口")
@RequestMapping("/api/v1")
public class NovaMallShoppingCartAPI {

    @Resource
    private NovaMallShoppingCartService novaMallShoppingCartService;

    @GetMapping("/shop-cart/page")
    @ApiOperation(value = "购物车列表(每页默认5条)", notes = "传参为页码")
    public Result<PageResult<List<NovaMallShoppingCartItemVO>>> cartItemPageList(Integer pageNumber, @TokenToMallUser MallUser loginMallUser) {
        Map params = new HashMap(8);
        if (pageNumber == null || pageNumber < 1) {
            pageNumber = 1;
        }
        params.put("userId", loginMallUser.getUserId());
        params.put("page", pageNumber);
        params.put("limit", Constants.SHOPPING_CART_PAGE_LIMIT);
        //封装分页请求参数
        PageQueryUtil pageUtil = new PageQueryUtil(params);
        return ResultGenerator.genSuccessResult(novaMallShoppingCartService.getMyShoppingCartItems(pageUtil));
    }

    @GetMapping("/shop-cart")
    @ApiOperation(value = "购物车列表(网页移动端不分页)", notes = "")
    public Result<List<NovaMallShoppingCartItemVO>> cartItemList(@TokenToMallUser MallUser loginMallUser) {
        return ResultGenerator.genSuccessResult(novaMallShoppingCartService.getMyShoppingCartItems(loginMallUser.getUserId()));
    }

    @PostMapping("/shop-cart")
    @ApiOperation(value = "添加商品到购物车接口", notes = "传参为商品id、数量")
    public Result saveNovaMallShoppingCartItem(@RequestBody SaveCartItemParam saveCartItemParam,
                                                 @TokenToMallUser MallUser loginMallUser) {
        String saveResult = novaMallShoppingCartService.saveNovaMallCartItem(saveCartItemParam, loginMallUser.getUserId());
        //添加成功
        if (ServiceResultEnum.SUCCESS.getResult().equals(saveResult)) {
            return ResultGenerator.genSuccessResult();
        }
        //添加失败
        return ResultGenerator.genFailResult(saveResult);
    }

    @PutMapping("/shop-cart")
    @ApiOperation(value = "修改购物项数据", notes = "传参为购物项id、数量")
    public Result updateNovaMallShoppingCartItem(@RequestBody UpdateCartItemParam updateCartItemParam,
                                                   @TokenToMallUser MallUser loginMallUser) {
        String updateResult = novaMallShoppingCartService.updateNovaMallCartItem(updateCartItemParam, loginMallUser.getUserId());
        //修改成功
        if (ServiceResultEnum.SUCCESS.getResult().equals(updateResult)) {
            return ResultGenerator.genSuccessResult();
        }
        //修改失败
        return ResultGenerator.genFailResult(updateResult);
    }

    @DeleteMapping("/shop-cart/{novaMallShoppingCartItemId}")
    @ApiOperation(value = "删除购物项", notes = "传参为购物项id")
    public Result updateNovaMallShoppingCartItem(@PathVariable("novaMallShoppingCartItemId") Long novaMallShoppingCartItemId,
                                                   @TokenToMallUser MallUser loginMallUser) {
        NovaMallShoppingCartItem novaMallCartItemById = novaMallShoppingCartService.getNovaMallCartItemById(novaMallShoppingCartItemId);
        if (!loginMallUser.getUserId().equals(novaMallCartItemById.getUserId())) {
            return ResultGenerator.genFailResult(ServiceResultEnum.REQUEST_FORBIDEN_ERROR.getResult());
        }
        Boolean deleteResult = novaMallShoppingCartService.deleteById(novaMallShoppingCartItemId,loginMallUser.getUserId());
        //删除成功
        if (deleteResult) {
            return ResultGenerator.genSuccessResult();
        }
        //删除失败
        return ResultGenerator.genFailResult(ServiceResultEnum.OPERATE_ERROR.getResult());
    }

    @GetMapping("/shop-cart/settle")
    @ApiOperation(value = "根据购物项id数组查询购物项明细", notes = "确认订单页面使用")
    public Result<List<NovaMallShoppingCartItemVO>> toSettle(Long[] cartItemIds, @TokenToMallUser MallUser loginMallUser) {
        if (cartItemIds.length < 1) {
            NovaMallException.fail("参数异常");
        }
        int priceTotal = 0;
        List<NovaMallShoppingCartItemVO> itemsForSettle = novaMallShoppingCartService.getCartItemsForSettle(Arrays.asList(cartItemIds), loginMallUser.getUserId());
        if (CollectionUtils.isEmpty(itemsForSettle)) {
            //无数据则抛出异常
            NovaMallException.fail("参数异常");
        } else {
            //总价
            for (NovaMallShoppingCartItemVO novaMallShoppingCartItemVO : itemsForSettle) {
                priceTotal += novaMallShoppingCartItemVO.getGoodsCount() * novaMallShoppingCartItemVO.getSellingPrice();
            }
            if (priceTotal < 1) {
                NovaMallException.fail("价格异常");
            }
        }
        return ResultGenerator.genSuccessResult(itemsForSettle);
    }
}

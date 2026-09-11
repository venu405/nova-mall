/**
 * 严肃声明：
 * 开源版本请务必保留此注释头信息，若删除我方将保留所有法律责任追究！
 * 本软件已申请软件著作权，受国家版权局知识产权以及国家计算机软件著作权保护！
 * 可正常分享和学习源码，不得用于违法犯罪活动，违者必究！
 * Copyright (c) 2019-2021 十三 all rights reserved.
 * 版权所有，侵权必究！
 */
package com.novamall.api.api.mall;

import io.swagger.annotations.*;
import com.novamall.api.api.mall.param.AiChatParam;
import com.novamall.api.api.mall.vo.NovaMallSearchGoodsVO;
import com.novamall.api.common.Constants;
import com.novamall.api.common.NovaMallException;
import com.novamall.api.common.ServiceResultEnum;
import com.novamall.api.config.annotation.TokenToMallUser;
import com.novamall.api.api.mall.vo.NovaMallGoodsDetailVO;
import com.novamall.api.entity.MallUser;
import com.novamall.api.entity.NovaMallGoods;
import com.novamall.api.service.NovaMallAiAssistantService;
import com.novamall.api.service.NovaMallGoodsService;
import com.novamall.api.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Api(value = "v1", tags = "4.NovaMall商城商品相关接口")
@RequestMapping("/api/v1")
public class NovaMallGoodsAPI {

    private static final Logger logger = LoggerFactory.getLogger(NovaMallGoodsAPI.class);

    @Resource
    private NovaMallGoodsService novaMallGoodsService;
    @Resource
    private NovaMallAiAssistantService novaMallAiAssistantService;

    @GetMapping("/search")
    @ApiOperation(value = "商品搜索接口", notes = "根据关键字和分类id进行搜索")
    public Result<PageResult<List<NovaMallSearchGoodsVO>>> search(@RequestParam(required = false) @ApiParam(value = "搜索关键字") String keyword,
                                                                    @RequestParam(required = false) @ApiParam(value = "分类id") Long goodsCategoryId,
                                                                    @RequestParam(required = false) @ApiParam(value = "orderBy") String orderBy,
                                                                    @RequestParam(required = false) @ApiParam(value = "页码") Integer pageNumber,
                                                                    @TokenToMallUser MallUser loginMallUser) {
        
        logger.info("goods search api,keyword={},goodsCategoryId={},orderBy={},pageNumber={},userId={}", keyword, goodsCategoryId, orderBy, pageNumber, loginMallUser.getUserId());

        Map params = new HashMap(8);
        //两个搜索参数都为空，直接返回异常
        if (goodsCategoryId == null && !StringUtils.hasText(keyword)) {
            NovaMallException.fail("非法的搜索参数");
        }
        if (pageNumber == null || pageNumber < 1) {
            pageNumber = 1;
        }
        params.put("goodsCategoryId", goodsCategoryId);
        params.put("page", pageNumber);
        params.put("limit", Constants.GOODS_SEARCH_PAGE_LIMIT);
        //对keyword做过滤 去掉空格
        if (StringUtils.hasText(keyword)) {
            params.put("keyword", keyword);
        }
        if (StringUtils.hasText(orderBy)) {
            params.put("orderBy", orderBy);
        }
        //搜索上架状态下的商品
        params.put("goodsSellStatus", Constants.SELL_STATUS_UP);
        //封装商品数据
        PageQueryUtil pageUtil = new PageQueryUtil(params);
        return ResultGenerator.genSuccessResult(novaMallGoodsService.searchNovaMallGoods(pageUtil));
    }

    @GetMapping("/goods/detail/{goodsId}")
    @ApiOperation(value = "商品详情接口", notes = "传参为商品id")
    public Result<NovaMallGoodsDetailVO> goodsDetail(@ApiParam(value = "商品id") @PathVariable("goodsId") Long goodsId, @TokenToMallUser MallUser loginMallUser) {
        logger.info("goods detail api,goodsId={},userId={}", goodsId, loginMallUser.getUserId());
        if (goodsId < 1) {
            return ResultGenerator.genFailResult("参数异常");
        }
        NovaMallGoods goods = novaMallGoodsService.getNovaMallGoodsById(goodsId);
        if (Constants.SELL_STATUS_UP != goods.getGoodsSellStatus()) {
            NovaMallException.fail(ServiceResultEnum.GOODS_PUT_DOWN.getResult());
        }
        NovaMallGoodsDetailVO goodsDetailVO = new NovaMallGoodsDetailVO();
        BeanUtil.copyProperties(goods, goodsDetailVO);
        goodsDetailVO.setGoodsCarouselList(goods.getGoodsCarousel().split(","));
        return ResultGenerator.genSuccessResult(goodsDetailVO);
    }

    @PostMapping("/goods/{goodsId}/ai-summary")
    @ApiOperation(value = "AI 商品简介接口", notes = "生成商品卖点简介，LLM 不可用时返回模板化兜底内容")
    public Result<List<String>> goodsAiSummary(@ApiParam(value = "商品id") @PathVariable("goodsId") Long goodsId, @TokenToMallUser MallUser loginMallUser) {
        logger.info("goods ai summary api,goodsId={},userId={}", goodsId, loginMallUser.getUserId());
        if (goodsId < 1) {
            return ResultGenerator.genFailResult("参数异常");
        }
        return ResultGenerator.genSuccessResult(novaMallAiAssistantService.generateGoodsSummary(goodsId));
    }

    @PostMapping("/goods/{goodsId}/ai-chat")
    @ApiOperation(value = "AI 商品问答接口", notes = "仅依据商品信息回答，LLM 不可用时返回兜底文案")
    public Result<String> goodsAiChat(@ApiParam(value = "商品id") @PathVariable("goodsId") Long goodsId,
                                      @RequestBody @Valid AiChatParam aiChatParam,
                                      @TokenToMallUser MallUser loginMallUser) {
        logger.info("goods ai chat api,goodsId={},userId={}", goodsId, loginMallUser.getUserId());
        if (goodsId < 1) {
            return ResultGenerator.genFailResult("参数异常");
        }
        return ResultGenerator.genSuccessResult(novaMallAiAssistantService.chatAboutGoods(goodsId, aiChatParam.getQuestion()));
    }

}

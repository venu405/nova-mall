/**
 * 严肃声明：
 * 开源版本请务必保留此注释头信息，若删除我方将保留所有法律责任追究！
 * 本软件已申请软件著作权，受国家版权局知识产权以及国家计算机软件著作权保护！
 * 可正常分享和学习源码，不得用于违法犯罪活动，违者必究！
 * Copyright (c) 2019-2021 十三 all rights reserved.
 * 版权所有，侵权必究！
 */
package com.novamall.api.service.impl;

import com.novamall.api.api.mall.vo.NovaMallSearchGoodsVO;
import com.novamall.api.cache.CacheConstants;
import com.novamall.api.cache.NovaMallCacheService;
import com.novamall.api.common.NovaMallCategoryLevelEnum;
import com.novamall.api.common.NovaMallException;
import com.novamall.api.common.ServiceResultEnum;
import com.novamall.api.dao.GoodsCategoryMapper;
import com.novamall.api.dao.NovaMallGoodsMapper;
import com.novamall.api.entity.GoodsCategory;
import com.novamall.api.entity.NovaMallGoods;
import com.novamall.api.service.NovaMallGoodsService;
import com.novamall.api.util.BeanUtil;
import com.novamall.api.util.PageQueryUtil;
import com.novamall.api.util.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class NovaMallGoodsServiceImpl implements NovaMallGoodsService {

    @Autowired
    private NovaMallGoodsMapper goodsMapper;
    @Autowired
    private GoodsCategoryMapper goodsCategoryMapper;
    @Autowired
    private NovaMallCacheService cacheService;

    @Override
    public PageResult getNovaMallGoodsPage(PageQueryUtil pageUtil) {
        List<NovaMallGoods> goodsList = goodsMapper.findNovaMallGoodsList(pageUtil);
        int total = goodsMapper.getTotalNovaMallGoods(pageUtil);
        PageResult pageResult = new PageResult(goodsList, total, pageUtil.getLimit(), pageUtil.getPage());
        return pageResult;
    }

    @Override
    public String saveNovaMallGoods(NovaMallGoods goods) {
        GoodsCategory goodsCategory = goodsCategoryMapper.selectByPrimaryKey(goods.getGoodsCategoryId());
        // 分类不存在或者不是三级分类，则该参数字段异常
        if (goodsCategory == null || goodsCategory.getCategoryLevel().intValue() != NovaMallCategoryLevelEnum.LEVEL_THREE.getLevel()) {
            return ServiceResultEnum.GOODS_CATEGORY_ERROR.getResult();
        }
        if (goodsMapper.selectByCategoryIdAndName(goods.getGoodsName(), goods.getGoodsCategoryId()) != null) {
            return ServiceResultEnum.SAME_GOODS_EXIST.getResult();
        }
        if (goodsMapper.insertSelective(goods) > 0) {
            return ServiceResultEnum.SUCCESS.getResult();
        }
        return ServiceResultEnum.DB_ERROR.getResult();
    }

    @Override
    public void batchSaveNovaMallGoods(List<NovaMallGoods> novaMallGoodsList) {
        if (!CollectionUtils.isEmpty(novaMallGoodsList)) {
            goodsMapper.batchInsert(novaMallGoodsList);
        }
    }

    @Override
    public String updateNovaMallGoods(NovaMallGoods goods) {
        GoodsCategory goodsCategory = goodsCategoryMapper.selectByPrimaryKey(goods.getGoodsCategoryId());
        // 分类不存在或者不是三级分类，则该参数字段异常
        if (goodsCategory == null || goodsCategory.getCategoryLevel().intValue() != NovaMallCategoryLevelEnum.LEVEL_THREE.getLevel()) {
            return ServiceResultEnum.GOODS_CATEGORY_ERROR.getResult();
        }
        NovaMallGoods temp = goodsMapper.selectByPrimaryKey(goods.getGoodsId());
        if (temp == null) {
            return ServiceResultEnum.DATA_NOT_EXIST.getResult();
        }
        NovaMallGoods temp2 = goodsMapper.selectByCategoryIdAndName(goods.getGoodsName(), goods.getGoodsCategoryId());
        if (temp2 != null && !temp2.getGoodsId().equals(goods.getGoodsId())) {
            //name和分类id相同且不同id 不能继续修改
            return ServiceResultEnum.SAME_GOODS_EXIST.getResult();
        }
        goods.setUpdateTime(new Date());
        if (goodsMapper.updateByPrimaryKeySelective(goods) > 0) {
            // 商品信息变更后删除商品详情及首页配置缓存，保证前台立即生效
            cacheService.delete(CacheConstants.GOODS_DETAIL_KEY + goods.getGoodsId());
            cacheService.deleteByPattern(CacheConstants.INDEX_CONFIG_GOODS_KEY + "*");
            return ServiceResultEnum.SUCCESS.getResult();
        }
        return ServiceResultEnum.DB_ERROR.getResult();
    }

    @Override
    public NovaMallGoods getNovaMallGoodsById(Long id) {
        String cacheKey = CacheConstants.GOODS_DETAIL_KEY + id;
        NovaMallGoods cachedGoods = cacheService.get(cacheKey, NovaMallGoods.class);
        if (cachedGoods != null) {
            // 命中空对象标记，说明该商品已确认不存在，直接拦截，防止缓存穿透
            if (cachedGoods.getGoodsId() == null) {
                NovaMallException.fail(ServiceResultEnum.GOODS_NOT_EXIST.getResult());
            }
            return cachedGoods;
        }
        NovaMallGoods novaMallGoods = goodsMapper.selectByPrimaryKey(id);
        if (novaMallGoods == null) {
            // 数据库也不存在时缓存空对象并设置短 TTL，避免恶意请求反复打穿到数据库
            cacheService.set(cacheKey, new NovaMallGoods(), CacheConstants.NULL_CACHE_TTL);
            NovaMallException.fail(ServiceResultEnum.GOODS_NOT_EXIST.getResult());
        }
        // 正常数据 TTL 加随机抖动，防止缓存雪崩
        cacheService.set(cacheKey, novaMallGoods, CacheConstants.randomTtl());
        return novaMallGoods;
    }

    @Override
    public Boolean batchUpdateSellStatus(Long[] ids, int sellStatus) {
        int result = goodsMapper.batchUpdateSellStatus(ids, sellStatus);
        if (result > 0) {
            // 上下架状态变更后删除商品详情及首页配置缓存，保证前台立即生效
            for (Long id : ids) {
                cacheService.delete(CacheConstants.GOODS_DETAIL_KEY + id);
            }
            cacheService.deleteByPattern(CacheConstants.INDEX_CONFIG_GOODS_KEY + "*");
        }
        return result > 0;
    }

    @Override
    public PageResult searchNovaMallGoods(PageQueryUtil pageUtil) {
        List<NovaMallGoods> goodsList = goodsMapper.findNovaMallGoodsListBySearch(pageUtil);
        int total = goodsMapper.getTotalNovaMallGoodsBySearch(pageUtil);
        List<NovaMallSearchGoodsVO> novaMallSearchGoodsVOS = new ArrayList<>();
        if (!CollectionUtils.isEmpty(goodsList)) {
            novaMallSearchGoodsVOS = BeanUtil.copyList(goodsList, NovaMallSearchGoodsVO.class);
            for (NovaMallSearchGoodsVO novaMallSearchGoodsVO : novaMallSearchGoodsVOS) {
                String goodsName = novaMallSearchGoodsVO.getGoodsName();
                String goodsIntro = novaMallSearchGoodsVO.getGoodsIntro();
                // 字符串过长导致文字超出的问题
                if (goodsName.length() > 28) {
                    goodsName = goodsName.substring(0, 28) + "...";
                    novaMallSearchGoodsVO.setGoodsName(goodsName);
                }
                if (goodsIntro.length() > 30) {
                    goodsIntro = goodsIntro.substring(0, 30) + "...";
                    novaMallSearchGoodsVO.setGoodsIntro(goodsIntro);
                }
            }
        }
        PageResult pageResult = new PageResult(novaMallSearchGoodsVOS, total, pageUtil.getLimit(), pageUtil.getPage());
        return pageResult;
    }
}

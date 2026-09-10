/**
 * 严肃声明：
 * 开源版本请务必保留此注释头信息，若删除我方将保留所有法律责任追究！
 * 本软件已申请软件著作权，受国家版权局知识产权以及国家计算机软件著作权保护！
 * 可正常分享和学习源码，不得用于违法犯罪活动，违者必究！
 * Copyright (c) 2019-2021 十三 all rights reserved.
 * 版权所有，侵权必究！
 */
package com.novamall.api.service.impl;

import com.novamall.api.api.mall.vo.NovaMallIndexConfigGoodsVO;
import com.novamall.api.cache.CacheConstants;
import com.novamall.api.cache.NovaMallCacheService;
import com.novamall.api.common.ServiceResultEnum;
import com.novamall.api.dao.IndexConfigMapper;
import com.novamall.api.dao.NovaMallGoodsMapper;
import com.novamall.api.entity.IndexConfig;
import com.novamall.api.entity.NovaMallGoods;
import com.novamall.api.service.NovaMallIndexConfigService;
import com.novamall.api.util.BeanUtil;
import com.novamall.api.util.PageQueryUtil;
import com.novamall.api.util.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NovaMallIndexConfigServiceImpl implements NovaMallIndexConfigService {

    @Autowired
    private IndexConfigMapper indexConfigMapper;

    @Autowired
    private NovaMallGoodsMapper goodsMapper;

    @Autowired
    private NovaMallCacheService cacheService;

    @Override
    public PageResult getConfigsPage(PageQueryUtil pageUtil) {
        List<IndexConfig> indexConfigs = indexConfigMapper.findIndexConfigList(pageUtil);
        int total = indexConfigMapper.getTotalIndexConfigs(pageUtil);
        PageResult pageResult = new PageResult(indexConfigs, total, pageUtil.getLimit(), pageUtil.getPage());
        return pageResult;
    }

    @Override
    public String saveIndexConfig(IndexConfig indexConfig) {
        if (goodsMapper.selectByPrimaryKey(indexConfig.getGoodsId()) == null) {
            return ServiceResultEnum.GOODS_NOT_EXIST.getResult();
        }
        if (indexConfigMapper.selectByTypeAndGoodsId(indexConfig.getConfigType(), indexConfig.getGoodsId()) != null) {
            return ServiceResultEnum.SAME_INDEX_CONFIG_EXIST.getResult();
        }
        if (indexConfigMapper.insertSelective(indexConfig) > 0) {
            // 首页配置变更后删除对应缓存，保证前台立即生效
            cacheService.deleteByPattern(CacheConstants.INDEX_CONFIG_GOODS_KEY + "*");
            return ServiceResultEnum.SUCCESS.getResult();
        }
        return ServiceResultEnum.DB_ERROR.getResult();
    }

    @Override
    public String updateIndexConfig(IndexConfig indexConfig) {
        if (goodsMapper.selectByPrimaryKey(indexConfig.getGoodsId()) == null) {
            return ServiceResultEnum.GOODS_NOT_EXIST.getResult();
        }
        IndexConfig temp = indexConfigMapper.selectByPrimaryKey(indexConfig.getConfigId());
        if (temp == null) {
            return ServiceResultEnum.DATA_NOT_EXIST.getResult();
        }
        IndexConfig temp2 = indexConfigMapper.selectByTypeAndGoodsId(indexConfig.getConfigType(), indexConfig.getGoodsId());
        if (temp2 != null && !temp2.getConfigId().equals(indexConfig.getConfigId())) {
            //goodsId相同且不同id 不能继续修改
            return ServiceResultEnum.SAME_INDEX_CONFIG_EXIST.getResult();
        }
        indexConfig.setUpdateTime(new Date());
        if (indexConfigMapper.updateByPrimaryKeySelective(indexConfig) > 0) {
            cacheService.deleteByPattern(CacheConstants.INDEX_CONFIG_GOODS_KEY + "*");
            return ServiceResultEnum.SUCCESS.getResult();
        }
        return ServiceResultEnum.DB_ERROR.getResult();
    }

    @Override
    public IndexConfig getIndexConfigById(Long id) {
        return indexConfigMapper.selectByPrimaryKey(id);
    }

    @Override
    public List<NovaMallIndexConfigGoodsVO> getConfigGoodsesForIndex(int configType, int number) {
        String cacheKey = CacheConstants.INDEX_CONFIG_GOODS_KEY + configType + ":" + number;
        // 首页配置商品为热点数据，优先读缓存；空列表也是有效缓存，防止缓存穿透
        List<NovaMallIndexConfigGoodsVO> cachedVOS = cacheService.getList(cacheKey, NovaMallIndexConfigGoodsVO.class);
        if (cachedVOS != null) {
            return cachedVOS;
        }
        List<NovaMallIndexConfigGoodsVO> novaMallIndexConfigGoodsVOS = new ArrayList<>(number);
        List<IndexConfig> indexConfigs = indexConfigMapper.findIndexConfigsByTypeAndNum(configType, number);
        if (!CollectionUtils.isEmpty(indexConfigs)) {
            //取出所有的goodsId
            List<Long> goodsIds = indexConfigs.stream().map(IndexConfig::getGoodsId).collect(Collectors.toList());
            List<NovaMallGoods> novaMallGoods = goodsMapper.selectByPrimaryKeys(goodsIds);
            novaMallIndexConfigGoodsVOS = BeanUtil.copyList(novaMallGoods, NovaMallIndexConfigGoodsVO.class);
            for (NovaMallIndexConfigGoodsVO novaMallIndexConfigGoodsVO : novaMallIndexConfigGoodsVOS) {
                String goodsName = novaMallIndexConfigGoodsVO.getGoodsName();
                String goodsIntro = novaMallIndexConfigGoodsVO.getGoodsIntro();
                // 字符串过长导致文字超出的问题
                if (goodsName.length() > 30) {
                    goodsName = goodsName.substring(0, 30) + "...";
                    novaMallIndexConfigGoodsVO.setGoodsName(goodsName);
                }
                if (goodsIntro.length() > 22) {
                    goodsIntro = goodsIntro.substring(0, 22) + "...";
                    novaMallIndexConfigGoodsVO.setGoodsIntro(goodsIntro);
                }
            }
        }
        // 无数据时设置短 TTL 的空列表，有数据时 TTL 加随机抖动防雪崩
        long ttl = CollectionUtils.isEmpty(novaMallIndexConfigGoodsVOS) ? CacheConstants.NULL_CACHE_TTL : CacheConstants.randomTtl();
        cacheService.set(cacheKey, novaMallIndexConfigGoodsVOS, ttl);
        return novaMallIndexConfigGoodsVOS;
    }

    @Override
    public Boolean deleteBatch(Long[] ids) {
        if (ids.length < 1) {
            return false;
        }
        //删除数据
        int result = indexConfigMapper.deleteBatch(ids);
        if (result > 0) {
            cacheService.deleteByPattern(CacheConstants.INDEX_CONFIG_GOODS_KEY + "*");
        }
        return result > 0;
    }
}

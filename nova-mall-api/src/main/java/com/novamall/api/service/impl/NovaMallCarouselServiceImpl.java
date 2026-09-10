/**
 * 严肃声明：
 * 开源版本请务必保留此注释头信息，若删除我方将保留所有法律责任追究！
 * 本软件已申请软件著作权，受国家版权局知识产权以及国家计算机软件著作权保护！
 * 可正常分享和学习源码，不得用于违法犯罪活动，违者必究！
 * Copyright (c) 2019-2021 十三 all rights reserved.
 * 版权所有，侵权必究！
 */
package com.novamall.api.service.impl;

import com.novamall.api.api.mall.vo.NovaMallIndexCarouselVO;
import com.novamall.api.cache.CacheConstants;
import com.novamall.api.cache.NovaMallCacheService;
import com.novamall.api.common.ServiceResultEnum;
import com.novamall.api.dao.CarouselMapper;
import com.novamall.api.entity.Carousel;
import com.novamall.api.service.NovaMallCarouselService;
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
public class NovaMallCarouselServiceImpl implements NovaMallCarouselService {

    @Autowired
    private CarouselMapper carouselMapper;
    @Autowired
    private NovaMallCacheService cacheService;


    @Override
    public PageResult getCarouselPage(PageQueryUtil pageUtil) {
        List<Carousel> carousels = carouselMapper.findCarouselList(pageUtil);
        int total = carouselMapper.getTotalCarousels(pageUtil);
        PageResult pageResult = new PageResult(carousels, total, pageUtil.getLimit(), pageUtil.getPage());
        return pageResult;
    }

    @Override
    public String saveCarousel(Carousel carousel) {
        if (carouselMapper.insertSelective(carousel) > 0) {
            // 轮播图变更后删除首页缓存，保证前台立即生效
            cacheService.deleteByPattern(CacheConstants.INDEX_CAROUSEL_KEY + "*");
            return ServiceResultEnum.SUCCESS.getResult();
        }
        return ServiceResultEnum.DB_ERROR.getResult();
    }

    @Override
    public String updateCarousel(Carousel carousel) {
        Carousel temp = carouselMapper.selectByPrimaryKey(carousel.getCarouselId());
        if (temp == null) {
            return ServiceResultEnum.DATA_NOT_EXIST.getResult();
        }
        temp.setCarouselRank(carousel.getCarouselRank());
        temp.setRedirectUrl(carousel.getRedirectUrl());
        temp.setCarouselUrl(carousel.getCarouselUrl());
        temp.setUpdateTime(new Date());
        if (carouselMapper.updateByPrimaryKeySelective(temp) > 0) {
            cacheService.deleteByPattern(CacheConstants.INDEX_CAROUSEL_KEY + "*");
            return ServiceResultEnum.SUCCESS.getResult();
        }
        return ServiceResultEnum.DB_ERROR.getResult();
    }

    @Override
    public Carousel getCarouselById(Integer id) {
        return carouselMapper.selectByPrimaryKey(id);
    }

    @Override
    public Boolean deleteBatch(Long[] ids) {
        if (ids.length < 1) {
            return false;
        }
        //删除数据
        int result = carouselMapper.deleteBatch(ids);
        if (result > 0) {
            cacheService.deleteByPattern(CacheConstants.INDEX_CAROUSEL_KEY + "*");
        }
        return result > 0;
    }

    @Override
    public List<NovaMallIndexCarouselVO> getCarouselsForIndex(int number) {
        String cacheKey = CacheConstants.INDEX_CAROUSEL_KEY + number;
        // 首页轮播图为热点数据，优先读缓存；空列表也是有效缓存，防止缓存穿透
        List<NovaMallIndexCarouselVO> cachedVOS = cacheService.getList(cacheKey, NovaMallIndexCarouselVO.class);
        if (cachedVOS != null) {
            return cachedVOS;
        }
        List<NovaMallIndexCarouselVO> novaMallIndexCarouselVOS = new ArrayList<>(number);
        List<Carousel> carousels = carouselMapper.findCarouselsByNum(number);
        if (!CollectionUtils.isEmpty(carousels)) {
            novaMallIndexCarouselVOS = BeanUtil.copyList(carousels, NovaMallIndexCarouselVO.class);
        }
        // 无数据时设置短 TTL 的空列表，有数据时 TTL 加随机抖动防雪崩
        long ttl = CollectionUtils.isEmpty(novaMallIndexCarouselVOS) ? CacheConstants.NULL_CACHE_TTL : CacheConstants.randomTtl();
        cacheService.set(cacheKey, novaMallIndexCarouselVOS, ttl);
        return novaMallIndexCarouselVOS;
    }
}

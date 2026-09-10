/**
 * 严肃声明：
 * 开源版本请务必保留此注释头信息，若删除我方将保留所有法律责任追究！
 * 本软件已申请软件著作权，受国家版权局知识产权以及国家计算机软件著作权保护！
 * 可正常分享和学习源码，不得用于违法犯罪活动，违者必究！
 * Copyright (c) 2019-2021 十三 all rights reserved.
 * 版权所有，侵权必究！
 */
package com.novamall.api.dao;

import com.novamall.api.entity.NovaMallGoods;
import com.novamall.api.entity.StockNumDTO;
import com.novamall.api.util.PageQueryUtil;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface NovaMallGoodsMapper {
    int deleteByPrimaryKey(Long goodsId);

    int insert(NovaMallGoods record);

    int insertSelective(NovaMallGoods record);

    NovaMallGoods selectByPrimaryKey(Long goodsId);

    NovaMallGoods selectByCategoryIdAndName(@Param("goodsName") String goodsName, @Param("goodsCategoryId") Long goodsCategoryId);

    int updateByPrimaryKeySelective(NovaMallGoods record);

    int updateByPrimaryKeyWithBLOBs(NovaMallGoods record);

    int updateByPrimaryKey(NovaMallGoods record);

    List<NovaMallGoods> findNovaMallGoodsList(PageQueryUtil pageUtil);

    int getTotalNovaMallGoods(PageQueryUtil pageUtil);

    List<NovaMallGoods> selectByPrimaryKeys(List<Long> goodsIds);

    List<NovaMallGoods> findNovaMallGoodsListBySearch(PageQueryUtil pageUtil);

    int getTotalNovaMallGoodsBySearch(PageQueryUtil pageUtil);

    int batchInsert(@Param("novaMallGoodsList") List<NovaMallGoods> novaMallGoodsList);

    int updateStockNum(@Param("stockNumDTOS") List<StockNumDTO> stockNumDTOS);

    int recoverStockNum(@Param("stockNumDTOS") List<StockNumDTO> stockNumDTOS);

    int batchUpdateSellStatus(@Param("orderIds")Long[] orderIds,@Param("sellStatus") int sellStatus);

}
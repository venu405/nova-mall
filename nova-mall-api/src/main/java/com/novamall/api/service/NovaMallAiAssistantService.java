package com.novamall.api.service;

import java.util.List;

/**
 * @apiNote AI 商品助手：商品简介生成与商品问答
 */
public interface NovaMallAiAssistantService {

    /**
     * 生成商品 AI 简介（3-5 条卖点），结果缓存 24 小时；
     * LLM 不可用时返回基于商品字段的模板化兜底简介
     *
     * @param goodsId
     * @return 卖点列表
     */
    List<String> generateGoodsSummary(Long goodsId);

    /**
     * 针对商品的自由问答，只允许依据商品信息回答，不缓存；
     * LLM 不可用时返回友好兜底文案
     *
     * @param goodsId
     * @param question
     * @return 回答文本
     */
    String chatAboutGoods(Long goodsId, String question);
}

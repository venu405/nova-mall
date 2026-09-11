package com.novamall.api.service.impl;

import com.novamall.api.cache.CacheConstants;
import com.novamall.api.cache.NovaMallCacheService;
import com.novamall.api.common.NovaMallException;
import com.novamall.api.common.ServiceResultEnum;
import com.novamall.api.dao.GoodsCategoryMapper;
import com.novamall.api.dao.NovaMallGoodsMapper;
import com.novamall.api.entity.GoodsCategory;
import com.novamall.api.entity.NovaMallGoods;
import com.novamall.api.service.NovaMallAiAssistantService;
import com.novamall.api.service.ai.LlmClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class NovaMallAiAssistantServiceImpl implements NovaMallAiAssistantService {

    private static final Logger logger = LoggerFactory.getLogger(NovaMallAiAssistantServiceImpl.class);

    /** 注入 Prompt 的商品详情文本最大长度，控制 token 成本 */
    private static final int DETAIL_TEXT_MAX_LENGTH = 2000;

    /** 用户问题最大长度，超长截断 */
    private static final int QUESTION_MAX_LENGTH = 500;

    /** 简介卖点最多保留条数 */
    private static final int SUMMARY_MAX_LINES = 5;

    private static final String SUMMARY_SYSTEM_PROMPT =
            "你是 NovaMall 积分商城的商品文案助手。请根据用户提供的商品信息，生成 3 到 5 条简洁的商品卖点简介。" +
            "要求：每条一行，不超过 40 个字，只输出卖点文本，不要输出标题、编号解释或其他多余内容；" +
            "卖点必须严格依据提供的商品信息，禁止编造商品参数、价格、功效或售后政策。";

    private static final String CHAT_SYSTEM_PROMPT =
            "你是 NovaMall 积分商城的商品问答助手。你只能根据用户消息中提供的商品信息回答问题，规则如下：\n" +
            "1. 商品信息中包含答案时，用简洁、友好的中文直接回答；\n" +
            "2. 商品信息不足以回答时，明确告知用户“根据现有商品信息暂时无法回答这个问题”，并建议用户查看商品详情或联系客服；\n" +
            "3. 严禁编造商品参数、价格、库存、物流时效、售后政策等任何未提供的信息；\n" +
            "4. 不回答与商品无关的问题，礼貌引导用户回到商品本身。";

    private static final String CHAT_FALLBACK_TEXT = "AI 助手暂时不可用，请稍后再试。您也可以查看商品详情页获取更多信息，或联系客服咨询。";

    @Autowired
    private NovaMallGoodsMapper novaMallGoodsMapper;
    @Autowired
    private GoodsCategoryMapper goodsCategoryMapper;
    @Autowired
    private NovaMallCacheService cacheService;
    @Autowired
    private LlmClient llmClient;

    @Override
    public List<String> generateGoodsSummary(Long goodsId) {
        String cacheKey = CacheConstants.GOODS_AI_SUMMARY_KEY + goodsId;
        // AI 生成内容对一致性不敏感，缓存 24 小时，商品编辑/下架时会主动删除该 key
        List<String> cachedSummary = cacheService.getList(cacheKey, String.class);
        if (cachedSummary != null && !cachedSummary.isEmpty()) {
            return cachedSummary;
        }
        NovaMallGoods goods = novaMallGoodsMapper.selectByPrimaryKey(goodsId);
        if (goods == null) {
            NovaMallException.fail(ServiceResultEnum.GOODS_NOT_EXIST.getResult());
        }
        List<String> summary = null;
        if (llmClient.isAvailable()) {
            String content = llmClient.chat(SUMMARY_SYSTEM_PROMPT,
                    "请为以下商品生成卖点简介：\n" + buildGoodsContext(goods));
            summary = parseSummaryLines(content);
        }
        if (summary == null || summary.isEmpty()) {
            // LLM 未启用或调用失败时，返回基于商品字段的模板化兜底简介
            logger.info("商品[{}]AI 简介使用模板化兜底内容", goodsId);
            summary = buildFallbackSummary(goods);
        }
        cacheService.set(cacheKey, summary, CacheConstants.AI_SUMMARY_TTL);
        return summary;
    }

    @Override
    public String chatAboutGoods(Long goodsId, String question) {
        NovaMallGoods goods = novaMallGoodsMapper.selectByPrimaryKey(goodsId);
        if (goods == null) {
            NovaMallException.fail(ServiceResultEnum.GOODS_NOT_EXIST.getResult());
        }
        if (!StringUtils.hasText(question)) {
            NovaMallException.fail(ServiceResultEnum.PARAM_ERROR.getResult());
        }
        // 超长问题截断，控制 token 成本
        String trimmedQuestion = question.trim();
        if (trimmedQuestion.length() > QUESTION_MAX_LENGTH) {
            trimmedQuestion = trimmedQuestion.substring(0, QUESTION_MAX_LENGTH);
        }
        if (!llmClient.isAvailable()) {
            return CHAT_FALLBACK_TEXT;
        }
        // 商品信息作为上下文注入，配合 System Prompt 的 grounding 约束防止编造
        String userPrompt = "【商品信息】\n" + buildGoodsContext(goods) + "\n【用户问题】\n" + trimmedQuestion;
        String answer = llmClient.chat(CHAT_SYSTEM_PROMPT, userPrompt);
        if (!StringUtils.hasText(answer)) {
            return CHAT_FALLBACK_TEXT;
        }
        return answer.trim();
    }

    /**
     * 组装注入 Prompt 的商品上下文：名称、简介、价格、分类、剥离 HTML 后的详情文本
     */
    private String buildGoodsContext(NovaMallGoods goods) {
        StringBuilder context = new StringBuilder();
        context.append("商品名称：").append(goods.getGoodsName()).append('\n');
        if (StringUtils.hasText(goods.getGoodsIntro())) {
            context.append("商品简介：").append(goods.getGoodsIntro()).append('\n');
        }
        context.append("商品价格：¥").append(goods.getSellingPrice())
                .append("（原价 ¥").append(goods.getOriginalPrice()).append("）\n");
        GoodsCategory category = goodsCategoryMapper.selectByPrimaryKey(goods.getGoodsCategoryId());
        if (category != null && StringUtils.hasText(category.getCategoryName())) {
            context.append("商品分类：").append(category.getCategoryName()).append('\n');
        }
        String detailText = stripHtml(goods.getGoodsDetailContent());
        if (StringUtils.hasText(detailText)) {
            context.append("商品详情：").append(detailText).append('\n');
        }
        return context.toString();
    }

    /**
     * 剥离 HTML 标签并压缩空白，截断到限定长度
     */
    private String stripHtml(String html) {
        if (!StringUtils.hasText(html)) {
            return "";
        }
        String text = html.replaceAll("<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replaceAll("\\s+", " ")
                .trim();
        if (text.length() > DETAIL_TEXT_MAX_LENGTH) {
            text = text.substring(0, DETAIL_TEXT_MAX_LENGTH);
        }
        return text;
    }

    /**
     * 将模型输出按行解析为卖点列表，去掉行首的序号/符号，最多保留限定条数
     */
    private List<String> parseSummaryLines(String content) {
        if (!StringUtils.hasText(content)) {
            return null;
        }
        List<String> lines = new ArrayList<>();
        for (String line : content.split("\n")) {
            String cleaned = line.trim().replaceAll("^\\s*(?:[-*•]+|\\d+\\s*[.、)])\\s*", "").trim();
            if (StringUtils.hasText(cleaned)) {
                lines.add(cleaned);
            }
            if (lines.size() >= SUMMARY_MAX_LINES) {
                break;
            }
        }
        return lines;
    }

    /**
     * 模板化兜底简介：不依赖 LLM，完全由商品字段拼装
     */
    private List<String> buildFallbackSummary(NovaMallGoods goods) {
        List<String> summary = new ArrayList<>();
        summary.add(goods.getGoodsName() + "，售价 ¥" + goods.getSellingPrice() + "（原价 ¥" + goods.getOriginalPrice() + "）。");
        if (StringUtils.hasText(goods.getGoodsIntro())) {
            summary.add(goods.getGoodsIntro() + "。");
        }
        GoodsCategory category = goodsCategoryMapper.selectByPrimaryKey(goods.getGoodsCategoryId());
        if (category != null && StringUtils.hasText(category.getCategoryName())) {
            summary.add("商品分类：" + category.getCategoryName() + "。");
        }
        summary.add("更多商品信息请查看详情页，或联系客服咨询。");
        return summary;
    }
}

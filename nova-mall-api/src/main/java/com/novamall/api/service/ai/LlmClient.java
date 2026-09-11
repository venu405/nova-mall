package com.novamall.api.service.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @apiNote OpenAI 兼容协议(/chat/completions)的 LLM 调用封装，
 * 配置项以 novamall.ai.* 注入；任何调用失败返回 null 并打 warn 日志，由调用方降级
 */
@Component
public class LlmClient {

    private static final Logger logger = LoggerFactory.getLogger(LlmClient.class);

    @Value("${novamall.ai.enabled:false}")
    private boolean enabled;

    @Value("${novamall.ai.base-url:}")
    private String baseUrl;

    @Value("${novamall.ai.api-key:}")
    private String apiKey;

    @Value("${novamall.ai.model:}")
    private String model;

    @Value("${novamall.ai.timeout-seconds:30}")
    private int timeoutSeconds;

    private RestTemplate restTemplate;

    @PostConstruct
    public void init() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutSeconds * 1000);
        requestFactory.setReadTimeout(timeoutSeconds * 1000);
        this.restTemplate = new RestTemplate(requestFactory);
    }

    /**
     * LLM 功能是否可用：开关打开且已配置 api-key、base-url
     */
    public boolean isAvailable() {
        return enabled && StringUtils.hasText(apiKey) && StringUtils.hasText(baseUrl) && StringUtils.hasText(model);
    }

    /**
     * 发起一轮对话，返回模型回复文本；不可用或调用失败时返回 null
     */
    @SuppressWarnings("unchecked")
    public String chat(String systemPrompt, String userPrompt) {
        if (!isAvailable()) {
            return null;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);
            messages.add(systemMessage);
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", userPrompt);
            messages.add(userMessage);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", messages);
            // 低温度降低随机性，配合 grounding 约束减少编造
            requestBody.put("temperature", 0.3);
            requestBody.put("stream", false);

            String url = baseUrl.endsWith("/") ? baseUrl + "chat/completions" : baseUrl + "/chat/completions";
            Map<String, Object> response = restTemplate.postForObject(url, new HttpEntity<>(requestBody, headers), Map.class);
            if (response == null) {
                return null;
            }
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty()) {
                return null;
            }
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            if (message == null) {
                return null;
            }
            Object content = message.get("content");
            return content == null ? null : content.toString();
        } catch (Exception e) {
            logger.warn("LLM 调用失败，将由调用方降级处理，model={}", model, e);
            return null;
        }
    }
}

package com.sky.ai.tool.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sky.ai.tool.AiFunctionDefinition;
import com.sky.ai.tool.AiFunctionHandler;
import com.sky.ai.tool.AiToolContext;
import com.sky.properties.FaqProperties;
import com.sky.properties.MerchantProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 智能客服 FAQ 工具。
 * <p>
 * 关键词匹配 FaqProperties.entries，命中的直接返回标准答案；未命中返回提示 + 客服热线。
 */
@Component
@Slf4j
public class CustomerServiceHandler implements AiFunctionHandler {

    @Autowired
    private FaqProperties faqProperties;
    @Autowired
    private MerchantProperties merchantProperties;

    @Override
    public String getName() {
        return "customer_service_faq";
    }

    @Override
    public String getRoleType() {
        return "USER";
    }

    @Override
    public AiFunctionDefinition getDefinition() {
        return AiFunctionDefinition.builder()
                .name("customer_service_faq")
                .description("回答常见客服问题：配送时间、配送费、起送价、营业时间、退款政策、支付方式、投诉建议、优惠活动等。返回标准答案。")
                .parametersJsonSchema("{\"type\":\"object\",\"properties\":{"
                        + "\"question\":{\"type\":\"string\",\"description\":\"用户的原始问题\"},"
                        + "\"topic\":{\"type\":\"string\",\"description\":\"问题主题，如：配送、退款、营业时间、支付、投诉、优惠\"}"
                        + "},\"required\":[\"question\"]}")
                .build();
    }

    @Override
    public String execute(String argumentsJson, AiToolContext context) {
        JSONObject args = JSON.parseObject(argumentsJson);
        String question = args.getString("question");
        String topic = args.getString("topic");

        if ((question == null || question.isEmpty()) && (topic == null || topic.isEmpty())) {
            return wrap(false, "请说明您想了解的问题，例如：配送时间多少？起送价多少？如何退款？",
                    null, merchantProperties.getServicePhone());
        }

        String haystack = ((question == null ? "" : question) + " " + (topic == null ? "" : topic)).toLowerCase();

        List<FaqProperties.Entry> entries = faqProperties.getEntries();
        if (entries != null) {
            for (FaqProperties.Entry entry : entries) {
                if (entry.getKeywords() == null || entry.getKeywords().isEmpty()) {
                    continue;
                }
                String[] keys = entry.getKeywords().split(",");
                for (String k : keys) {
                    if (!k.trim().isEmpty() && haystack.contains(k.trim().toLowerCase())) {
                        JSONObject out = new JSONObject();
                        out.put("matched", true);
                        out.put("category", entry.getCategory());
                        out.put("answer", entry.getAnswer());
                        out.put("hotline", merchantProperties.getServicePhone());
                        return out.toJSONString();
                    }
                }
            }
        }

        // 未命中：给出可问的分类 + 兜底
        JSONArray suggestions = new JSONArray();
        if (entries != null) {
            for (FaqProperties.Entry e : entries) {
                if (e.getCategory() != null) {
                    suggestions.add(e.getCategory());
                }
            }
        }
        JSONObject out = new JSONObject();
        out.put("matched", false);
        out.put("message", "暂未找到标准答案，您可以尝试以下关键词：" + suggestions.toJSONString()
                + "，或拨打客服热线 " + merchantProperties.getServicePhone() + "。");
        out.put("hotline", merchantProperties.getServicePhone());
        return out.toJSONString();
    }

    private String wrap(boolean matched, String msg, String category, String hotline) {
        JSONObject out = new JSONObject();
        out.put("matched", matched);
        out.put("message", msg);
        if (category != null) {
            out.put("category", category);
        }
        out.put("hotline", hotline);
        return out.toJSONString();
    }
}

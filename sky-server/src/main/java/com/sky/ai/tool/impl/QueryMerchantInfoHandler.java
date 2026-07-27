package com.sky.ai.tool.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.ai.tool.AiFunctionDefinition;
import com.sky.ai.tool.AiFunctionHandler;
import com.sky.ai.tool.AiToolContext;
import com.sky.properties.MerchantProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

/**
 * 商家信息查询工具：起送价 / 配送费 / 营业时间 / 配送范围 / 退款政策 等。
 * 用户问"现在还在营业吗？"这类需要结合当前时间的判断时也由 LLM 触发。
 */
@Component
@Slf4j
public class QueryMerchantInfoHandler implements AiFunctionHandler {

    @Autowired
    private MerchantProperties merchantProperties;

    @Override
    public String getName() {
        return "query_merchant_info";
    }

    @Override
    public String getRoleType() {
        return "USER";
    }

    @Override
    public AiFunctionDefinition getDefinition() {
        return AiFunctionDefinition.builder()
                .name("query_merchant_info")
                .description("查询商家基础信息：起送价、配送费、配送范围、营业时间、配送时长、退款政策、客服电话等。")
                .parametersJsonSchema("{\"type\":\"object\",\"properties\":{"
                        + "\"fields\":{\"type\":\"array\",\"items\":{\"type\":\"string\"},"
                        + "\"description\":\"可选字段列表：name/deliveryFee/minOrderAmount/deliveryRange/avgDeliveryMinutes/openTime/closeTime/servicePhone/refundPolicy/description；不传则返回全部\"}"
                        + "},\"required\":[]}")
                .build();
    }

    @Override
    public String execute(String argumentsJson, AiToolContext context) {
        JSONObject args = argumentsJson == null || argumentsJson.isEmpty()
                ? new JSONObject() : JSON.parseObject(argumentsJson);

        JSONObject out = new JSONObject();
        out.put("name", merchantProperties.getName());
        out.put("description", merchantProperties.getDescription());
        out.put("deliveryFee", merchantProperties.getDeliveryFee());
        out.put("minOrderAmount", merchantProperties.getMinOrderAmount());
        out.put("deliveryRange", merchantProperties.getDeliveryRange());
        out.put("avgDeliveryMinutes", merchantProperties.getAvgDeliveryMinutes());
        out.put("openTime", merchantProperties.getOpenTime());
        out.put("closeTime", merchantProperties.getCloseTime());
        out.put("servicePhone", merchantProperties.getServicePhone());
        out.put("refundPolicy", merchantProperties.getRefundPolicy());
        out.put("now", LocalTime.now().withNano(0).toString());
        out.put("isOpenNow", isOpenNow(merchantProperties.getOpenTime(), merchantProperties.getCloseTime()));

        // 如果指定了 fields 则裁剪
        var fields = args.getJSONArray("fields");
        if (fields != null && !fields.isEmpty()) {
            JSONObject filtered = new JSONObject();
            for (int i = 0; i < fields.size(); i++) {
                String f = fields.getString(i);
                if (out.containsKey(f)) {
                    filtered.put(f, out.get(f));
                }
            }
            return filtered.toJSONString();
        }
        return out.toJSONString();
    }

    private boolean isOpenNow(String open, String close) {
        try {
            LocalTime now = LocalTime.now();
            LocalTime o = LocalTime.parse(open);
            LocalTime c = LocalTime.parse(close);
            // 不处理跨天
            return !now.isBefore(o) && !now.isAfter(c);
        } catch (Exception e) {
            return true;
        }
    }
}

package com.sky.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 商家可配置的 FAQ 知识库。
 * <p>
 * 在 application.yml 中以列表形式配置，每条包含：
 * <ul>
 *     <li>keywords：触发该 FAQ 的关键词（任一命中即匹配）</li>
 *     <li>category：问题分类（配送/退款/支付/营业/投诉/优惠/会员 等）</li>
 *     <li>answer：标准答案</li>
 * </ul>
 * 命中 FAQ 时直接返回标准答案；未命中则由 LLM 自由回答或引导用户换关键词。
 */
@Component
@ConfigurationProperties(prefix = "sky.faq")
@Data
public class FaqProperties {

    /**
     * 客服热线，注入 system prompt / 兜底回复
     */
    private String hotLine = "400-123-4567";

    /**
     * FAQ 列表
     */
    private List<Entry> entries = new ArrayList<>();

    @Data
    public static class Entry {
        /** 触发关键词，多个用英文逗号分隔 */
        private String keywords;
        /** 分类，便于 LLM 二次归类 */
        private String category;
        /** 标准答案 */
        private String answer;
    }
}

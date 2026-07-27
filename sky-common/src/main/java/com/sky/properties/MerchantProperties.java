package com.sky.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 商家基础信息，AI 客服与商家画像共用。
 * <p>
 * 真实生产中可改为数据库存储 + 启动加载；这里以 yml 配置满足 demo。
 */
@Component
@ConfigurationProperties(prefix = "sky.merchant")
@Data
public class MerchantProperties {

    /** 商家名称 */
    private String name = "苍穹外卖";

    /** 公告 / 简介 */
    private String description = "苍穹外卖为您提供优质餐品，30-60 分钟内送达。";

    /** 配送费（元） */
    private Double deliveryFee = 5.0;

    /** 起送价（元） */
    private Double minOrderAmount = 20.0;

    /** 配送范围（公里） */
    private Double deliveryRange = 3.0;

    /** 平均配送时长（分钟） */
    private Integer avgDeliveryMinutes = 45;

    /** 营业开始时间，HH:mm */
    private String openTime = "09:00";

    /** 营业结束时间，HH:mm */
    private String closeTime = "22:00";

    /** 客服电话 */
    private String servicePhone = "400-123-4567";

    /** 退款政策 */
    private String refundPolicy = "未接单可全额退款；已接单未派送需商家确认；已派送仅在质量问题下支持，1-3 工作日原路退回。";
}

package com.sky.ai.tool.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sky.ai.tool.AiFunctionDefinition;
import com.sky.ai.tool.AiFunctionHandler;
import com.sky.ai.tool.AiToolContext;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import com.sky.properties.MerchantProperties;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 管理端 AI 自动审单 / 投诉处置工具。
 * <p>
 * 输入：orderId（必填），可选 complaintType / reason / proposedAction。
 * 输出（结构化建议，不直接改状态）：
 * <ul>
 *     <li>订单与用户画像：金额、下单时间、状态、用户最近取消/拒单频次</li>
 *     <li>风险等级：low / medium / high</li>
 *     <li>建议动作：confirm(接单) / reject(拒单) / cancel(取消并退款) / keep(保持现状) / escalate(转人工)</li>
 *     <li>命中规则清单（用户/金额/时效/关键词）</li>
 *     <li>参考回复话术（中文）</li>
 * </ul>
 */
@Component
@Slf4j
public class AdminReviewOrderHandler implements AiFunctionHandler {

    @Autowired
    private OrderService orderService;
    @Autowired
    private MerchantProperties merchantProperties;

    private static final Set<String> HIGH_RISK_KEYWORDS = Set.of("吃出", "虫子", "头发", "异物", "食物中毒", "拉肚子", "腹泻", "呕吐", "过敏", "送错", "漏送", "变质", "发霉", "投诉");
    private static final Set<String> MEDIUM_RISK_KEYWORDS = Set.of("凉了", "冷了", "洒了", "漏了", "迟到", "超时", "太慢", "味道差", "难吃");

    @Override
    public String getName() {
        return "admin_review_order";
    }

    @Override
    public String getRoleType() {
        return "ADMIN";
    }

    @Override
    public AiFunctionDefinition getDefinition() {
        return AiFunctionDefinition.builder()
                .name("admin_review_order")
                .description("管理端自动审单 / 投诉处置：拉取订单详情 + 用户近 50 单取消/拒单频次 + 时效 + 关键词，给出风险等级、建议动作（confirm/reject/cancel/keep/escalate）与参考话术。不会直接修改订单状态，需管理员确认后自行调用 admin_manage_order。")
                .parametersJsonSchema("{\"type\":\"object\",\"properties\":{"
                        + "\"orderId\":{\"type\":\"integer\",\"description\":\"订单ID\"},"
                        + "\"complaintType\":{\"type\":\"string\",\"description\":\"投诉类型：quality(质量问题) / delivery(配送问题) / wrong(错送漏送) / other\"},"
                        + "\"reason\":{\"type\":\"string\",\"description\":\"用户描述/客服填写的投诉原因/退款理由原文\"},"
                        + "\"proposedAction\":{\"type\":\"string\",\"description\":\"若已有处理方向：confirm/reject/cancel/keep/escalate；不传则由 AI 自行判断\"}"
                        + "},\"required\":[\"orderId\"]}")
                .build();
    }

    @Override
    public String execute(String argumentsJson, AiToolContext context) {
        JSONObject args = argumentsJson == null || argumentsJson.isEmpty()
                ? new JSONObject() : JSON.parseObject(argumentsJson);
        Long orderId = args.getLong("orderId");
        if (orderId == null) {
            return "{\"error\": \"订单ID不能为空\"}";
        }
        String complaintType = args.getString("complaintType");
        String reason = args.getString("reason");
        String proposedAction = args.getString("proposedAction");

        // 1) 订单详情
        OrderVO order;
        try {
            order = orderService.details(orderId);
        } catch (Exception e) {
            log.warn("订单不存在或查询失败 orderId={}", orderId, e);
            return "{\"error\": \"订单不存在或查询失败: " + e.getMessage() + "\"}";
        }
        if (order == null) {
            return "{\"error\": \"订单不存在\"}";
        }

        // 2) 用户画像：拉最近 50 单，统计 cancel/reject 频次
        int userCancelCount = 0;
        int userRejectCount = 0;
        int userTotalCount = 0;
        try {
            OrdersPageQueryDTO q = new OrdersPageQueryDTO();
            q.setUserId(order.getUserId());
            q.setPage(1);
            q.setPageSize(50);
            PageResult pr = orderService.conditionSearch(q);
            if (pr != null && pr.getRecords() != null) {
                List<Orders> list = (List<Orders>) pr.getRecords();
                userTotalCount = list.size();
                for (Orders o : list) {
                    if (o.getStatus() == null) continue;
                    if (o.getStatus() == Orders.CANCELLED) userCancelCount++;
                    if (o.getRejectionReason() != null && !o.getRejectionReason().isEmpty()) userRejectCount++;
                }
            }
        } catch (Exception e) {
            log.warn("拉取用户历史订单失败 orderId={} userId={}", orderId, order.getUserId(), e);
        }

        // 3) 时效：从下单到现在的分钟数
        long minutesSinceOrder = 0;
        if (order.getOrderTime() != null) {
            minutesSinceOrder = Duration.between(order.getOrderTime(), LocalDateTime.now()).toMinutes();
        }

        // 4) 关键词命中
        String text = (reason == null ? "" : reason) + " "
                + (order.getCancelReason() == null ? "" : order.getCancelReason()) + " "
                + (order.getRejectionReason() == null ? "" : order.getRejectionReason());
        JSONArray keywordHits = new JSONArray();
        for (String k : HIGH_RISK_KEYWORDS) {
            if (text.contains(k)) keywordHits.add(JSON.toJSON(java.util.Map.of("keyword", k, "level", "high")));
        }
        for (String k : MEDIUM_RISK_KEYWORDS) {
            if (text.contains(k)) keywordHits.add(JSON.toJSON(java.util.Map.of("keyword", k, "level", "medium")));
        }

        // 5) 规则评估
        JSONArray rules = new JSONArray();
        BigDecimal amount = order.getAmount() == null ? BigDecimal.ZERO : order.getAmount();
        int orderStatus = order.getStatus() == null ? 0 : order.getStatus();

        rules.add(rule("order_status", orderStatus, "当前订单状态：" + describeStatus(orderStatus)));
        rules.add(rule("amount", amount, "订单金额 " + amount + " 元"));
        rules.add(rule("minutes_since_order", minutesSinceOrder, "已下单 " + minutesSinceOrder + " 分钟"));
        rules.add(rule("user_total_orders", userTotalCount, "用户近 50 单共 " + userTotalCount + " 单"));
        rules.add(rule("user_cancel_count", userCancelCount, "用户近 50 单取消 " + userCancelCount + " 单"));
        rules.add(rule("user_reject_count", userRejectCount, "用户近 50 单被拒 " + userRejectCount + " 单"));
        rules.add(rule("keyword_hits", keywordHits.size(), "投诉关键词命中 " + keywordHits.size() + " 个"));
        if (complaintType != null) {
            rules.add(rule("complaint_type", complaintType, "投诉类型：" + complaintType));
        }

        // 6) 风险打分（0-100）
        int riskScore = 0;
        List<String> reasons4Risk = new ArrayList<>();
        if (keywordHits.size() > 0) {
            // 有 high 关键词 +40，每个 +10
            boolean hasHigh = false;
            for (int i = 0; i < keywordHits.size(); i++) {
                if ("high".equals(keywordHits.getJSONObject(i).getString("level"))) {
                    hasHigh = true; break;
                }
            }
            if (hasHigh) { riskScore += 40; reasons4Risk.add("命中高风险关键词（食品安全/卫生/中毒等）"); }
            else { riskScore += 15; reasons4Risk.add("命中中等风险关键词（配送/口感）"); }
        }
        if (userCancelCount >= 3) {
            riskScore += 20; reasons4Risk.add("用户近 50 单取消 ≥ 3 次（高频取消）");
        } else if (userCancelCount == 2) {
            riskScore += 10; reasons4Risk.add("用户近 50 单取消 2 次");
        }
        if (userRejectCount >= 2) {
            riskScore += 15; reasons4Risk.add("用户近 50 单被拒 ≥ 2 次");
        }
        if (amount != null && amount.compareTo(new BigDecimal("100")) > 0) {
            riskScore += 10; reasons4Risk.add("订单金额 > 100 元，高客单需谨慎");
        }
        if (orderStatus == Orders.TO_BE_CONFIRMED && minutesSinceOrder > 10) {
            riskScore += 5; reasons4Risk.add("待接单已超过 10 分钟，体验分风险");
        }
        if ("quality".equalsIgnoreCase(complaintType)) {
            riskScore += 15; reasons4Risk.add("投诉类型为质量问题");
        }
        if (riskScore > 100) riskScore = 100;

        String riskLevel;
        if (riskScore >= 60) riskLevel = "high";
        else if (riskScore >= 30) riskLevel = "medium";
        else riskLevel = "low";

        // 7) 建议动作（仅建议）
        String suggested;
        if (proposedAction != null && !proposedAction.isEmpty()) {
            suggested = proposedAction;
        } else {
            suggested = autoSuggest(orderStatus, riskLevel, complaintType, keywordHits);
        }

        // 8) 参考话术
        String script = composeScript(order, reason, suggested, riskLevel);

        // 9) 输出
        JSONObject out = new JSONObject();
        out.put("orderId", order.getId());
        out.put("orderNumber", order.getNumber());
        out.put("orderStatus", orderStatus);
        out.put("orderStatusText", describeStatus(orderStatus));
        out.put("amount", amount);
        out.put("orderTime", order.getOrderTime() == null ? null : order.getOrderTime().toString());
        out.put("minutesSinceOrder", minutesSinceOrder);
        out.put("consignee", order.getConsignee());
        out.put("phone", order.getPhone());
        out.put("address", order.getAddress());
        out.put("cancelReason", order.getCancelReason());
        out.put("rejectionReason", order.getRejectionReason());
        out.put("remark", order.getRemark());

        JSONObject userProfile = new JSONObject();
        userProfile.put("userId", order.getUserId());
        userProfile.put("userName", order.getUserName());
        userProfile.put("recentTotal", userTotalCount);
        userProfile.put("recentCancel", userCancelCount);
        userProfile.put("recentReject", userRejectCount);
        out.put("userProfile", userProfile);

        out.put("rules", rules);
        out.put("keywordHits", keywordHits);
        out.put("riskScore", riskScore);
        out.put("riskLevel", riskLevel);
        out.put("riskReasons", reasons4Risk);
        out.put("suggestedAction", suggested);
        out.put("suggestedActionText", describeAction(suggested));
        out.put("replyScript", script);
        out.put("hotline", merchantProperties.getServicePhone());
        out.put("note", "本工具只给出建议，不会直接修改订单状态。请管理员确认后调用 admin_manage_order 执行。");

        return out.toJSONString();
    }

    private JSONObject rule(String k, Object v, String desc) {
        JSONObject o = new JSONObject();
        o.put("key", k);
        o.put("value", v);
        o.put("desc", desc);
        return o;
    }

    private String autoSuggest(int status, String riskLevel, String complaintType, JSONArray keywordHits) {
        if ("quality".equalsIgnoreCase(complaintType) || riskLevel.equals("high")) {
            // 高风险 / 质量问题：建议直接退款 + 道歉
            return "cancel";
        }
        if (riskLevel.equals("medium")) {
            if (status == Orders.TO_BE_CONFIRMED) return "cancel";
            return "escalate";
        }
        // low
        if (status == Orders.TO_BE_CONFIRMED) return "confirm";
        if (status == Orders.PENDING_PAYMENT) return "keep";
        return "keep";
    }

    private String composeScript(OrderVO order, String reason, String suggested, String riskLevel) {
        String name = order.getConsignee() == null ? "尊敬的顾客" : order.getConsignee();
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("您好，我是").append(merchantProperties.getName())
                .append("客服。关于您订单【").append(order.getNumber()).append("】的反馈");
        if (reason != null && !reason.isEmpty()) {
            sb.append("（\"").append(reason).append("\"）");
        }
        sb.append("，我们深表");
        if ("high".equals(riskLevel)) sb.append("歉意");
        else if ("medium".equals(riskLevel)) sb.append("抱歉");
        else sb.append("关注");
        sb.append("。");
        switch (suggested) {
            case "cancel":
                sb.append("已为您办理全额退款，预计 1-3 个工作日原路退回；")
                  .append("同时我们会复盘出餐与配送流程，杜绝再次发生。");
                break;
            case "reject":
                sb.append("因订单超出配送范围 / 当前无法接单，已为您取消并全额退款。");
                break;
            case "confirm":
                sb.append("您的订单已确认，骑手正在前往商家取餐，请耐心等待。");
                break;
            case "escalate":
                sb.append("情况较复杂，我们已升级至值班主管复核，将在 30 分钟内主动联系您。");
                break;
            default:
                sb.append("我们会持续关注订单进展，有任何问题可拨打客服 ")
                  .append(merchantProperties.getServicePhone()).append("。");
        }
        return sb.toString();
    }

    private String describeStatus(int s) {
        switch (s) {
            case 1: return "待付款";
            case 2: return "待接单";
            case 3: return "已接单";
            case 4: return "派送中";
            case 5: return "已完成";
            case 6: return "已取消";
            default: return "未知(" + s + ")";
        }
    }

    private String describeAction(String a) {
        switch (a) {
            case "confirm": return "接单";
            case "reject": return "拒单";
            case "cancel": return "取消并退款";
            case "delivery": return "派送";
            case "complete": return "完成";
            case "escalate": return "转人工复核";
            case "keep":
            default: return "保持现状";
        }
    }
}

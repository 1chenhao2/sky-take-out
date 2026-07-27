package com.sky.ai.tool.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sky.ai.tool.AiFunctionDefinition;
import com.sky.ai.tool.AiFunctionHandler;
import com.sky.ai.tool.AiToolContext;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理端 AI 经营洞察工具。
 * <p>
 * 输入：日期范围 + period（today / 7d / 30d 三档快捷值，留空则按 beginDate/endDate 计算）。
 * 输出：
 * <ul>
 *     <li>当前周期 vs 上一个等长周期的 营业额/订单/用户 环比</li>
 *     <li>每日明细（日期数组）</li>
 *     <li>销量 top10 概要</li>
 *     <li>高峰日/低峰日（按营业额与订单数）</li>
 * </ul>
 * 纯结构化数据返回，由 LLM 进一步汇总成"结论+数据依据+建议"。
 */
@Component
@Slf4j
public class AdminInsightHandler implements AiFunctionHandler {

    @Autowired
    private ReportService reportService;

    @Override
    public String getName() {
        return "admin_insight";
    }

    @Override
    public String getRoleType() {
        return "ADMIN";
    }

    @Override
    public AiFunctionDefinition getDefinition() {
        return AiFunctionDefinition.builder()
                .name("admin_insight")
                .description("管理端经营洞察：拉取营业额/订单/用户统计与销量 top10，计算当前周期 vs 上一个等长周期的环比，并标记高峰/低峰日。返回结构化数据供 AI 总结。")
                .parametersJsonSchema("{\"type\":\"object\",\"properties\":{"
                        + "\"beginDate\":{\"type\":\"string\",\"description\":\"开始日期 yyyy-MM-dd，可选\"},"
                        + "\"endDate\":{\"type\":\"string\",\"description\":\"结束日期 yyyy-MM-dd，可选\"},"
                        + "\"period\":{\"type\":\"string\",\"description\":\"快捷周期：today / 7d / 30d，优先级低于 beginDate/endDate\"}"
                        + "},\"required\":[]}")
                .build();
    }

    @Override
    public String execute(String argumentsJson, AiToolContext context) {
        JSONObject args = argumentsJson == null || argumentsJson.isEmpty()
                ? new JSONObject() : JSON.parseObject(argumentsJson);
        String beginStr = args.getString("beginDate");
        String endStr = args.getString("endDate");
        String period = args.getString("period");

        LocalDate today = LocalDate.now();
        LocalDate begin, end;
        if (beginStr != null && endStr != null) {
            begin = LocalDate.parse(beginStr);
            end = LocalDate.parse(endStr);
        } else if (period != null) {
            switch (period.toLowerCase()) {
                case "today":
                    begin = today; end = today; break;
                case "7d":
                    end = today; begin = today.minusDays(6); break;
                case "30d":
                default:
                    end = today; begin = today.minusDays(29); break;
            }
        } else {
            // 默认 7 天
            end = today; begin = today.minusDays(6);
        }

        if (begin.isAfter(end)) {
            return "{\"error\": \"开始日期不能晚于结束日期\"}";
        }

        // 上一个等长周期：[beginPrev, endPrev]，长度 = (end - begin) + 1
        long days = end.toEpochDay() - begin.toEpochDay() + 1;
        LocalDate endPrev = begin.minusDays(1);
        LocalDate beginPrev = endPrev.minusDays(days - 1);

        // 拉数
        TurnoverReportVO curTurnover = reportService.getTurnoverStatistics(begin, end);
        OrderReportVO curOrder = reportService.getOrderStatistics(begin, end);
        UserReportVO curUser = reportService.getUserStatistics(begin, end);
        SalesTop10ReportVO top10 = reportService.getSalesTop10(begin, end);

        TurnoverReportVO prevTurnover = reportService.getTurnoverStatistics(beginPrev, endPrev);
        OrderReportVO prevOrder = reportService.getOrderStatistics(beginPrev, endPrev);
        UserReportVO prevUser = reportService.getUserStatistics(beginPrev, endPrev);

        // 计算
        double curSum = sumList(curTurnover.getTurnoverList());
        double prevSum = sumList(prevTurnover.getTurnoverList());
        int curOrders = curOrder.getTotalOrderCount() == null ? 0 : curOrder.getTotalOrderCount();
        int prevOrders = prevOrder.getTotalOrderCount() == null ? 0 : prevOrder.getTotalOrderCount();
        int curValid = curOrder.getValidOrderCount() == null ? 0 : curOrder.getValidOrderCount();
        int prevValid = prevOrder.getValidOrderCount() == null ? 0 : prevOrder.getValidOrderCount();
        int curNewUser = sumListInt(curUser.getNewUserList());
        int prevNewUser = sumListInt(prevUser.getNewUserList());

        JSONObject result = new JSONObject();
        result.put("currentRange", begin + " ~ " + end);
        result.put("previousRange", beginPrev + " ~ " + endPrev);

        JSONObject turnover = new JSONObject();
        turnover.put("currentTotal", curSum);
        turnover.put("previousTotal", prevSum);
        turnover.put("delta", curSum - prevSum);
        turnover.put("growthRate", rate(curSum, prevSum));
        turnover.put("dailyList", dailyPairs(curTurnover.getDateList(), curTurnover.getTurnoverList()));
        result.put("turnover", turnover);

        JSONObject order = new JSONObject();
        order.put("currentTotal", curOrders);
        order.put("previousTotal", prevOrders);
        order.put("delta", curOrders - prevOrders);
        order.put("growthRate", rate(curOrders, prevOrders));
        order.put("currentValid", curValid);
        order.put("previousValid", prevValid);
        order.put("validDelta", curValid - prevValid);
        order.put("validGrowthRate", rate(curValid, prevValid));
        order.put("completionRate", curOrder.getOrderCompletionRate());
        order.put("dailyOrderList", dailyPairs(curOrder.getDateList(), curOrder.getOrderCountList()));
        order.put("dailyValidOrderList", dailyPairs(curOrder.getDateList(), curOrder.getValidOrderCountList()));
        result.put("order", order);

        JSONObject user = new JSONObject();
        user.put("currentNewUser", curNewUser);
        user.put("previousNewUser", prevNewUser);
        user.put("delta", curNewUser - prevNewUser);
        user.put("growthRate", rate(curNewUser, prevNewUser));
        user.put("dailyNewUserList", dailyPairs(curUser.getDateList(), curUser.getNewUserList()));
        result.put("user", user);

        result.put("topSales", top10);

        // 高峰 / 低峰
        result.put("turnoverPeak", peak(curTurnover.getDateList(), curTurnover.getTurnoverList(), true));
        result.put("turnoverTrough", peak(curTurnover.getDateList(), curTurnover.getTurnoverList(), false));
        result.put("orderPeak", peak(curOrder.getDateList(), curOrder.getOrderCountList(), true));
        result.put("orderTrough", peak(curOrder.getDateList(), curOrder.getOrderCountList(), false));

        return result.toJSONString();
    }

    private double sumList(String csv) {
        if (csv == null || csv.isEmpty()) return 0d;
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .mapToDouble(s -> {
                    try { return Double.parseDouble(s); } catch (Exception e) { return 0d; }
                }).sum();
    }

    private int sumListInt(String csv) {
        if (csv == null || csv.isEmpty()) return 0;
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .mapToInt(s -> {
                    try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
                }).sum();
    }

    private String rate(double cur, double prev) {
        if (prev == 0d) {
            return cur == 0d ? "0%" : "+∞";
        }
        return String.format("%+.2f%%", (cur - prev) / prev * 100);
    }

    private List<JSONObject> dailyPairs(String dateCsv, String valueCsv) {
        if (dateCsv == null || valueCsv == null) return List.of();
        String[] dates = dateCsv.split(",");
        String[] vals = valueCsv.split(",");
        return java.util.stream.IntStream.range(0, Math.min(dates.length, vals.length))
                .mapToObj(i -> {
                    JSONObject o = new JSONObject();
                    o.put("date", dates[i].trim());
                    o.put("value", vals[i].trim());
                    return o;
                }).collect(Collectors.toList());
    }

    private JSONObject peak(String dateCsv, String valueCsv, boolean high) {
        if (dateCsv == null || valueCsv == null || dateCsv.isEmpty() || valueCsv.isEmpty()) {
            return new JSONObject();
        }
        String[] dates = dateCsv.split(",");
        String[] vals = valueCsv.split(",");
        int idx = -1;
        double best = high ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
        for (int i = 0; i < Math.min(dates.length, vals.length); i++) {
            double v;
            try { v = Double.parseDouble(vals[i].trim()); } catch (Exception e) { continue; }
            if (high ? v > best : v < best) {
                best = v;
                idx = i;
            }
        }
        JSONObject out = new JSONObject();
        if (idx >= 0) {
            out.put("date", dates[idx].trim());
            out.put("value", vals[idx].trim());
        }
        return out;
    }
}

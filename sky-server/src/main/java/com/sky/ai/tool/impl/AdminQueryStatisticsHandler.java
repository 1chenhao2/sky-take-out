package com.sky.ai.tool.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.ai.tool.AiFunctionDefinition;
import com.sky.ai.tool.AiFunctionHandler;
import com.sky.ai.tool.AiToolContext;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@Slf4j
public class AdminQueryStatisticsHandler implements AiFunctionHandler {

    @Autowired
    private ReportService reportService;

    @Override
    public String getName() {
        return "admin_query_statistics";
    }

    @Override
    public String getRoleType() {
        return "ADMIN";
    }

    @Override
    public AiFunctionDefinition getDefinition() {
        return AiFunctionDefinition.builder()
                .name("admin_query_statistics")
                .description("获取指定日期范围内的营业额、订单和用户统计数据")
                .parametersJsonSchema("{\"type\":\"object\",\"properties\":{\"beginDate\":{\"type\":\"string\",\"description\":\"开始日期，格式yyyy-MM-dd\"},\"endDate\":{\"type\":\"string\",\"description\":\"结束日期，格式yyyy-MM-dd\"}},\"required\":[\"beginDate\",\"endDate\"]}")
                .build();
    }

    @Override
    public String execute(String argumentsJson, AiToolContext context) {
        JSONObject args = JSON.parseObject(argumentsJson);
        String beginStr = args.getString("beginDate");
        String endStr = args.getString("endDate");

        if (beginStr == null || endStr == null) {
            return "{\"error\": \"开始日期和结束日期不能为空\"}";
        }

        LocalDate begin = LocalDate.parse(beginStr);
        LocalDate end = LocalDate.parse(endStr);

        JSONObject result = new JSONObject();

        TurnoverReportVO turnover = reportService.getTurnoverStatistics(begin, end);
        result.put("turnover", JSON.toJSON(turnover));

        OrderReportVO orderReport = reportService.getOrderStatistics(begin, end);
        result.put("orderReport", JSON.toJSON(orderReport));

        UserReportVO userReport = reportService.getUserStatistics(begin, end);
        result.put("userReport", JSON.toJSON(userReport));

        return result.toJSONString();
    }
}

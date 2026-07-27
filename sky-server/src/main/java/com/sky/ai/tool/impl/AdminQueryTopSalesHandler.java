package com.sky.ai.tool.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.ai.tool.AiFunctionDefinition;
import com.sky.ai.tool.AiFunctionHandler;
import com.sky.ai.tool.AiToolContext;
import com.sky.service.ReportService;
import com.sky.vo.SalesTop10ReportVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@Slf4j
public class AdminQueryTopSalesHandler implements AiFunctionHandler {

    @Autowired
    private ReportService reportService;

    @Override
    public String getName() {
        return "admin_query_top_sales";
    }

    @Override
    public String getRoleType() {
        return "ADMIN";
    }

    @Override
    public AiFunctionDefinition getDefinition() {
        return AiFunctionDefinition.builder()
                .name("admin_query_top_sales")
                .description("获取指定日期范围内的销量前十排名")
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

        SalesTop10ReportVO salesTop10 = reportService.getSalesTop10(begin, end);
        return JSON.toJSONString(salesTop10);
    }
}

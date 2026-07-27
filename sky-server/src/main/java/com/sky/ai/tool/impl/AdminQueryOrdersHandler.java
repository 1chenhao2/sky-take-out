package com.sky.ai.tool.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sky.ai.tool.AiFunctionDefinition;
import com.sky.ai.tool.AiFunctionHandler;
import com.sky.ai.tool.AiToolContext;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Component
@Slf4j
public class AdminQueryOrdersHandler implements AiFunctionHandler {

    @Autowired
    private OrderService orderService;

    @Override
    public String getName() {
        return "admin_query_orders";
    }

    @Override
    public String getRoleType() {
        return "ADMIN";
    }

    @Override
    public AiFunctionDefinition getDefinition() {
        return AiFunctionDefinition.builder()
                .name("admin_query_orders")
                .description("按条件搜索订单，支持按状态、日期范围、订单号筛选")
                .parametersJsonSchema("{\"type\":\"object\",\"properties\":{\"status\":{\"type\":\"integer\",\"description\":\"订单状态：1待付款 2待接单 3已接单 4派送中 5已完成 6已取消\"},\"beginDate\":{\"type\":\"string\",\"description\":\"开始日期，格式yyyy-MM-dd\"},\"endDate\":{\"type\":\"string\",\"description\":\"结束日期，格式yyyy-MM-dd\"},\"number\":{\"type\":\"string\",\"description\":\"订单号\"},\"page\":{\"type\":\"integer\",\"description\":\"页码，默认1\"},\"pageSize\":{\"type\":\"integer\",\"description\":\"每页数量，默认10\"}},\"required\":[]}")
                .build();
    }

    @Override
    public String execute(String argumentsJson, AiToolContext context) {
        JSONObject args = JSON.parseObject(argumentsJson);
        OrdersPageQueryDTO dto = new OrdersPageQueryDTO();
        dto.setPage(args.getIntValue("page") > 0 ? args.getIntValue("page") : 1);
        dto.setPageSize(args.getIntValue("pageSize") > 0 ? args.getIntValue("pageSize") : 10);
        if (args.getInteger("status") != null) {
            dto.setStatus(args.getInteger("status"));
        }
        if (args.getString("number") != null) {
            dto.setNumber(args.getString("number"));
        }
        if (args.getString("beginDate") != null) {
            LocalDate beginDate = LocalDate.parse(args.getString("beginDate"));
            dto.setBeginTime(LocalDateTime.of(beginDate, LocalTime.MIN));
        }
        if (args.getString("endDate") != null) {
            LocalDate endDate = LocalDate.parse(args.getString("endDate"));
            dto.setEndTime(LocalDateTime.of(endDate, LocalTime.MAX));
        }

        PageResult pageResult = orderService.conditionSearch(dto);
        List<OrderVO> orders = (List<OrderVO>) pageResult.getRecords();

        JSONArray records = new JSONArray();
        for (OrderVO order : orders) {
            JSONObject item = new JSONObject();
            item.put("orderId", order.getId());
            item.put("orderNumber", order.getNumber());
            item.put("status", order.getStatus());
            item.put("amount", order.getAmount());
            item.put("userName", order.getUserName());
            item.put("phone", order.getPhone());
            item.put("orderTime", order.getOrderTime() != null ? order.getOrderTime().toString() : null);
            records.add(item);
        }

        JSONObject result = new JSONObject();
        result.put("total", pageResult.getTotal());
        result.put("orders", records);
        return result.toJSONString();
    }
}

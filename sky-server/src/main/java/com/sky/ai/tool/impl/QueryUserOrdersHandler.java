package com.sky.ai.tool.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sky.ai.tool.AiFunctionDefinition;
import com.sky.ai.tool.AiFunctionHandler;
import com.sky.ai.tool.AiToolContext;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class QueryUserOrdersHandler implements AiFunctionHandler {

    @Autowired
    private OrderService orderService;

    @Override
    public String getName() {
        return "query_my_orders";
    }

    @Override
    public String getRoleType() {
        return "USER";
    }

    @Override
    public AiFunctionDefinition getDefinition() {
        return AiFunctionDefinition.builder()
                .name("query_my_orders")
                .description("查询当前用户的订单列表，可按状态筛选")
                .parametersJsonSchema("{\"type\":\"object\",\"properties\":{\"status\":{\"type\":\"integer\",\"description\":\"订单状态：1待付款 2待接单 3已接单 4派送中 5已完成 6已取消，不传表示全部\"},\"page\":{\"type\":\"integer\",\"description\":\"页码，默认1\"},\"pageSize\":{\"type\":\"integer\",\"description\":\"每页数量，默认10\"}},\"required\":[]}")
                .build();
    }

    @Override
    public String execute(String argumentsJson, AiToolContext context) {
        JSONObject args = JSON.parseObject(argumentsJson);
        int page = args.getIntValue("page") > 0 ? args.getIntValue("page") : 1;
        int pageSize = args.getIntValue("pageSize") > 0 ? args.getIntValue("pageSize") : 10;
        Integer status = args.getInteger("status");

        PageResult pageResult = orderService.pageQuery4User(page, pageSize, status);
        List<OrderVO> orders = (List<OrderVO>) pageResult.getRecords();

        JSONArray result = new JSONArray();
        for (OrderVO order : orders) {
            JSONObject item = new JSONObject();
            item.put("orderId", order.getId());
            item.put("orderNumber", order.getNumber());
            item.put("status", order.getStatus());
            item.put("amount", order.getAmount());
            item.put("orderTime", order.getOrderTime() != null ? order.getOrderTime().toString() : null);
            item.put("dishes", order.getOrderDishes());
            result.add(item);
        }

        JSONObject response = new JSONObject();
        response.put("total", pageResult.getTotal());
        response.put("orders", result);
        return response.toJSONString();
    }
}

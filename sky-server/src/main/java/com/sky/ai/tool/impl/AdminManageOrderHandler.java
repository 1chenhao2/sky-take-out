package com.sky.ai.tool.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.ai.tool.AiFunctionDefinition;
import com.sky.ai.tool.AiFunctionHandler;
import com.sky.ai.tool.AiToolContext;
import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AdminManageOrderHandler implements AiFunctionHandler {

    @Autowired
    private OrderService orderService;

    @Override
    public String getName() {
        return "admin_manage_order";
    }

    @Override
    public String getRoleType() {
        return "ADMIN";
    }

    @Override
    public AiFunctionDefinition getDefinition() {
        return AiFunctionDefinition.builder()
                .name("admin_manage_order")
                .description("处理订单操作：接单、拒单、取消、完成、派送")
                .parametersJsonSchema("{\"type\":\"object\",\"properties\":{\"orderId\":{\"type\":\"integer\",\"description\":\"订单ID\"},\"action\":{\"type\":\"string\",\"description\":\"操作类型：confirm(接单)、reject(拒单)、cancel(取消)、complete(完成)、delivery(派送)\"},\"reason\":{\"type\":\"string\",\"description\":\"拒单或取消时的原因\"}},\"required\":[\"orderId\",\"action\"]}")
                .build();
    }

    @Override
    public String execute(String argumentsJson, AiToolContext context) {
        JSONObject args = JSON.parseObject(argumentsJson);
        Long orderId = args.getLong("orderId");
        String action = args.getString("action");
        String reason = args.getString("reason");

        if (orderId == null || action == null) {
            return "{\"error\": \"订单ID和操作类型不能为空\"}";
        }

        try {
            switch (action.toLowerCase()) {
                case "confirm": {
                    OrdersConfirmDTO dto = new OrdersConfirmDTO();
                    dto.setId(orderId);
                    orderService.confirm(dto);
                    return "{\"success\": true, \"message\": \"订单【" + orderId + "】已接单\"}";
                }
                case "reject": {
                    OrdersRejectionDTO dto = new OrdersRejectionDTO();
                    dto.setId(orderId);
                    dto.setRejectionReason(reason != null ? reason : "其他原因");
                    orderService.rejection(dto);
                    return "{\"success\": true, \"message\": \"订单【" + orderId + "】已拒单\"}";
                }
                case "cancel": {
                    OrdersCancelDTO dto = new OrdersCancelDTO();
                    dto.setId(orderId);
                    dto.setCancelReason(reason != null ? reason : "其他原因");
                    orderService.cancel(dto);
                    return "{\"success\": true, \"message\": \"订单【" + orderId + "】已取消\"}";
                }
                case "complete":
                    orderService.complete(orderId);
                    return "{\"success\": true, \"message\": \"订单【" + orderId + "】已完成\"}";
                case "delivery":
                    orderService.delivery(orderId);
                    return "{\"success\": true, \"message\": \"订单【" + orderId + "】已开始派送\"}";
                default:
                    return "{\"error\": \"不支持的操作类型：【" + action + "】，支持的操作：confirm、reject、cancel、complete、delivery\"}";
            }
        } catch (Exception e) {
            log.error("订单操作失败", e);
            return "{\"error\": \"订单操作失败：" + e.getMessage() + "\"}";
        }
    }
}

package com.sky.ai.tool.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.ai.tool.AiFunctionDefinition;
import com.sky.ai.tool.AiFunctionHandler;
import com.sky.ai.tool.AiToolContext;
import com.sky.service.WorkSpaceService;
import com.sky.vo.BusinessDataVO;
import com.sky.vo.DishOverViewVO;
import com.sky.vo.OrderOverViewVO;
import com.sky.vo.SetmealOverViewVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Component
@Slf4j
public class AdminQueryOverviewHandler implements AiFunctionHandler {

    @Autowired
    private WorkSpaceService workSpaceService;

    @Override
    public String getName() {
        return "admin_query_overview";
    }

    @Override
    public String getRoleType() {
        return "ADMIN";
    }

    @Override
    public AiFunctionDefinition getDefinition() {
        return AiFunctionDefinition.builder()
                .name("admin_query_overview")
                .description("获取今日运营概览数据，包括营业额、订单概览、菜品/套餐概览")
                .parametersJsonSchema("{\"type\":\"object\",\"properties\":{},\"required\":[]}")
                .build();
    }

    @Override
    public String execute(String argumentsJson, AiToolContext context) {
        LocalDate today = LocalDate.now();
        LocalDateTime beginTime = LocalDateTime.of(today, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(today, LocalTime.MAX);

        BusinessDataVO businessData = workSpaceService.getBusinessData(beginTime, endTime);
        OrderOverViewVO orderOverview = workSpaceService.getOverviewOrders();
        DishOverViewVO dishOverview = workSpaceService.getOverviewDishes();
        SetmealOverViewVO setmealOverview = workSpaceService.getOverviewSetmeals();

        JSONObject result = new JSONObject();
        result.put("businessData", JSON.toJSON(businessData));
        result.put("orderOverview", JSON.toJSON(orderOverview));
        result.put("dishOverview", JSON.toJSON(dishOverview));
        result.put("setmealOverview", JSON.toJSON(setmealOverview));

        return result.toJSONString();
    }
}

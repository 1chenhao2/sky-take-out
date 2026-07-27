package com.sky.ai.tool.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.ai.tool.AiFunctionDefinition;
import com.sky.ai.tool.AiFunctionHandler;
import com.sky.ai.tool.AiToolContext;
import com.sky.entity.Dish;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class QueryDishDetailHandler implements AiFunctionHandler {

    @Autowired
    private DishService dishService;

    @Override
    public String getName() {
        return "query_dish_detail";
    }

    @Override
    public String getRoleType() {
        return "USER";
    }

    @Override
    public AiFunctionDefinition getDefinition() {
        return AiFunctionDefinition.builder()
                .name("query_dish_detail")
                .description("根据菜品名称查询菜品详细信息，包括价格、描述和口味")
                .parametersJsonSchema("{\"type\":\"object\",\"properties\":{\"dishName\":{\"type\":\"string\",\"description\":\"菜品名称\"}},\"required\":[\"dishName\"]}")
                .build();
    }

    @Override
    public String execute(String argumentsJson, AiToolContext context) {
        JSONObject args = JSON.parseObject(argumentsJson);
        String dishName = args.getString("dishName");

        if (dishName == null || dishName.isEmpty()) {
            return "{\"error\": \"菜品名称不能为空\"}";
        }

        Dish queryParam = Dish.builder().name(dishName).build();
        try {
            java.util.List<DishVO> dishes = dishService.listWithFlavor(queryParam);
            if (dishes.isEmpty()) {
                return "{\"message\": \"未找到名为【" + dishName + "】的菜品\"}";
            }
            DishVO dish = dishes.get(0);
            JSONObject result = new JSONObject();
            result.put("name", dish.getName());
            result.put("price", dish.getPrice());
            result.put("description", dish.getDescription());
            result.put("status", dish.getStatus());
            result.put("image", dish.getImage());
            if (dish.getFlavors() != null && !dish.getFlavors().isEmpty()) {
                result.put("flavors", JSON.toJSONString(dish.getFlavors()));
            }
            return result.toJSONString();
        } catch (Exception e) {
            log.error("查询菜品详情失败", e);
            return "{\"error\": \"查询菜品详情失败\"}";
        }
    }
}

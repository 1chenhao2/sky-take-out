package com.sky.ai.tool.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sky.ai.tool.AiFunctionDefinition;
import com.sky.ai.tool.AiFunctionHandler;
import com.sky.ai.tool.AiToolContext;
import com.sky.entity.Category;
import com.sky.entity.Dish;
import com.sky.service.CategoryService;
import com.sky.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class RecommendDishesHandler implements AiFunctionHandler {

    @Autowired
    private DishService dishService;
    @Autowired
    private CategoryService categoryService;

    @Override
    public String getName() {
        return "recommend_dishes";
    }

    @Override
    public String getRoleType() {
        return "USER";
    }

    @Override
    public AiFunctionDefinition getDefinition() {
        return AiFunctionDefinition.builder()
                .name("recommend_dishes")
                .description("根据分类名称推荐菜品，返回菜品名称、价格和描述")
                .parametersJsonSchema("{\"type\":\"object\",\"properties\":{\"category\":{\"type\":\"string\",\"description\":\"菜品分类名称，如：热菜、凉菜、汤羹、主食、饮品\"},\"limit\":{\"type\":\"integer\",\"description\":\"返回菜品数量上限，默认5\"}},\"required\":[]}")
                .build();
    }

    @Override
    public String execute(String argumentsJson, AiToolContext context) {
        JSONObject args = JSON.parseObject(argumentsJson);
        String categoryName = args.getString("category");
        int limit = args.getIntValue("limit") > 0 ? args.getIntValue("limit") : 5;

        JSONArray result = new JSONArray();
        if (categoryName != null && !categoryName.isEmpty()) {
            List<Category> categories = categoryService.list(1);
            Long categoryId = null;
            for (Category c : categories) {
                if (c.getName().contains(categoryName)) {
                    categoryId = c.getId();
                    break;
                }
            }
            if (categoryId != null) {
                List<Dish> dishes = dishService.list(categoryId);
                int count = 0;
                for (Dish dish : dishes) {
                    if (dish.getStatus() == 1) {
                        JSONObject item = new JSONObject();
                        item.put("name", dish.getName());
                        item.put("price", dish.getPrice());
                        item.put("description", dish.getDescription());
                        item.put("categoryId", dish.getCategoryId());
                        result.add(item);
                        count++;
                        if (count >= limit) break;
                    }
                }
            }
        }

        // If no specific category, return all enabled dishes up to limit
        if (result.isEmpty()) {
            List<Category> categories = categoryService.list(1);
            int count = 0;
            for (Category c : categories) {
                List<Dish> dishes = dishService.list(c.getId());
                for (Dish dish : dishes) {
                    if (dish.getStatus() == 1) {
                        JSONObject item = new JSONObject();
                        item.put("name", dish.getName());
                        item.put("price", dish.getPrice());
                        item.put("description", dish.getDescription());
                        item.put("categoryId", dish.getCategoryId());
                        result.add(item);
                        count++;
                        if (count >= limit) break;
                    }
                }
                if (count >= limit) break;
            }
        }

        return result.toJSONString();
    }
}

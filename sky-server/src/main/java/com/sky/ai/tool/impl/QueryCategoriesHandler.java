package com.sky.ai.tool.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sky.ai.tool.AiFunctionDefinition;
import com.sky.ai.tool.AiFunctionHandler;
import com.sky.ai.tool.AiToolContext;
import com.sky.entity.Category;
import com.sky.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class QueryCategoriesHandler implements AiFunctionHandler {

    @Autowired
    private CategoryService categoryService;

    @Override
    public String getName() {
        return "query_categories";
    }

    @Override
    public String getRoleType() {
        return "USER";
    }

    @Override
    public AiFunctionDefinition getDefinition() {
        return AiFunctionDefinition.builder()
                .name("query_categories")
                .description("获取所有可用的菜品分类列表")
                .parametersJsonSchema("{\"type\":\"object\",\"properties\":{},\"required\":[]}")
                .build();
    }

    @Override
    public String execute(String argumentsJson, AiToolContext context) {
        List<Category> categories = categoryService.list(1);
        JSONArray result = new JSONArray();
        for (Category c : categories) {
            if (c.getStatus() == 1) {
                JSONObject item = new JSONObject();
                item.put("id", c.getId());
                item.put("name", c.getName());
                item.put("sort", c.getSort());
                result.add(item);
            }
        }
        return result.toJSONString();
    }
}

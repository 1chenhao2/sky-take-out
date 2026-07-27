package com.sky.ai.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ToolRegistry {

    @Autowired
    private List<AiFunctionHandler> handlers;

    private Map<String, AiFunctionHandler> handlerMap = new HashMap<>();

    @PostConstruct
    public void init() {
        for (AiFunctionHandler handler : handlers) {
            handlerMap.put(handler.getName(), handler);
            log.info("注册AI工具: {} (角色: {})", handler.getName(), handler.getRoleType());
        }
    }

    public AiFunctionHandler getHandler(String name) {
        return handlerMap.get(name);
    }

    public List<AiFunctionDefinition> getDefinitionsForRole(String roleType) {
        return handlers.stream()
                .filter(h -> roleType.equals(h.getRoleType()))
                .map(AiFunctionHandler::getDefinition)
                .collect(Collectors.toList());
    }

}

package com.sky.ai.tool;

public interface AiFunctionHandler {

    String getName();

    String getRoleType();

    AiFunctionDefinition getDefinition();

    String execute(String argumentsJson, AiToolContext context);

}

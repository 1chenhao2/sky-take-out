package com.sky.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sky.ai")
@Data
public class AiProperties {

    private String apiUrl = "https://api.deepseek.com/v1";

    private String apiKey;

    private String model = "deepseek-chat";

    private int maxTokens = 2000;

    private double temperature = 0.7;

    private int conversationTtl = 30;

    private int maxHistoryMessages = 40;

}

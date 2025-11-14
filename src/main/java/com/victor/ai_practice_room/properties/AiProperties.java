package com.victor.ai_practice_room.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
@Data
@ConfigurationProperties(prefix = "ai.api")
public class AiProperties {

    private String model;
    private String baseUrl;
    private String apiKey;
    private Integer maxTokens;
    private Double temperature;
}
package com.sunrise.web.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "app.api.rate-limit")
public class ApiRateLimitProperties {
    private boolean enabled = true;
    private int defaultLimit = 300;
    private Map<String, Integer> urls = new HashMap<>(); // паттерн -> лимит
}
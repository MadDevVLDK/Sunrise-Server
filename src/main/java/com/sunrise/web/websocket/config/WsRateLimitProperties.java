package com.sunrise.web.websocket.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "app.websocket.rate-limit")
public class WsRateLimitProperties {
    private boolean enabled = true;
    private int defaultLimit = 60;
    private Map<String, Integer> commands = new HashMap<>();
}
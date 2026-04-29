package com.sunrise.web.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sunrise.cache.service.StatisticsCacheService;

import java.util.Map;


@RestController
@RequiredArgsConstructor
public class HealthTestController {

    private final StatisticsCacheService cacheService;


    @GetMapping("/ping")
    public Map<String, String> getStatus() {
        return Map.of(
            "name", "Sunrise-Server",
            "status", "🟢 Онлайн",
            "version", "0.3"
        );
    }

    @GetMapping("/cache-status")
    public ResponseEntity<?> getCashStatus() {
        return ResponseEntity.ok(cacheService.getCacheStatus());
    }
}
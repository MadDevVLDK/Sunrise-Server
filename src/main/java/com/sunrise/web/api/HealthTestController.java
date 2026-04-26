package com.sunrise.web.api;

import com.sunrise.dataservice.DataOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RequiredArgsConstructor
@RestController
public class HealthTestController {

    private final DataOrchestrator dataOrchestrator;

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
        return ResponseEntity.ok(dataOrchestrator.getCacheStatus());
    }
}
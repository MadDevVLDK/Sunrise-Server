package com.sunrise.web.payload;

import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Map;

@lombok.Getter
@lombok.AllArgsConstructor
public class ApiResponse<T> {
    private final boolean success;
    private final T data;
    private final String error;
    private final Map<String, Object> meta = Map.of("api-version", "Sunrise v1.0");
    private final Instant timestamp = Instant.now();

    // Фабрика успеха
    public static <T> ResponseEntity<ApiResponse<T>> success() {
        return ResponseEntity.ok(new ApiResponse<>(true, null, null));
    }
    public static <T> ResponseEntity<ApiResponse<T>> success(T data) {
        return ResponseEntity.ok(new ApiResponse<>(true, data, null));
    }
    public static <T> ResponseEntity<ApiResponse<T>> error(String error) {
        return ResponseEntity.badRequest().body(new ApiResponse<>(false, null, error));
    }
}
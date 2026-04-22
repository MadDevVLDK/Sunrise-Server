package com.sunrise.config.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    @Value("${app.rate-limit.requests-per-minute:300}")
    private int maxRequestsPerMinute;

    private final Cache<String, Integer> rateLimitCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .maximumSize(10000) // максимум 10000 запросов с одного IP
            .build();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String clientIp = getClientIp(request);
        Integer currentRequests = rateLimitCache.getIfPresent(clientIp);
        if (currentRequests == null) {
            currentRequests = 0;
        }

        if (currentRequests >= maxRequestsPerMinute) {
            log.warn("[⚠️] Rate limit exceeded for IP: {}, requests: {}/{}", clientIp, currentRequests, maxRequestsPerMinute);
            response.setStatus(429); // HTTP 429 Too Many Requests
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"Too many requests. Please try again later.\",\"retryAfter\":60}");
            response.getWriter().flush();
            return;
        }

        // Увеличиваем счетчик
        rateLimitCache.put(clientIp, currentRequests + 1);
        
        // Добавляем информацию об оставшихся запросах в ответ
        response.addHeader("X-RateLimit-Limit", String.valueOf(maxRequestsPerMinute));
        response.addHeader("X-RateLimit-Remaining", String.valueOf(maxRequestsPerMinute - currentRequests - 1));
        response.addHeader("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() + 60000));

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return Objects.requireNonNullElse(request.getRemoteAddr(), "UNKNOWN");
    }
}
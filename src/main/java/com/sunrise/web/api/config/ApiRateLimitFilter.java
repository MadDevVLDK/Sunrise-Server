package com.sunrise.web.api.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
public class ApiRateLimitFilter extends OncePerRequestFilter {

    private final ApiRateLimitProperties properties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // key -> "userId:urlPattern" || "ip:urlPattern"
    private final Cache<String, Integer> rateLimitCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .maximumSize(10000)
            .build();


    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (!properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String requestUri = request.getRequestURI();
        int limit = getLimitForUri(requestUri);
        if (limit <= 0) {
            filterChain.doFilter(request, response);
            return;
        }

        // Пытаемся получить userId из SecurityContext (устанавливается JwtFilter)
        String identifier = getIdentifier(request);
        String key = identifier + ":" + requestUri;
        Integer currentRequests = rateLimitCache.get(key, k -> 0);

        if (currentRequests >= limit) {
            log.warn("[API RateLimit] Blocked {} at URI: {}, count: {}/{}", identifier, requestUri, currentRequests, limit);
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(String.format("{\"error\":\"Too many requests to %s. Limit: %d per minute\",\"retryAfter\":60}", requestUri, limit));
            response.getWriter().flush();
            return;
        }

        rateLimitCache.put(key, currentRequests + 1);

        response.addHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.addHeader("X-RateLimit-Remaining", String.valueOf(limit - currentRequests - 1));
        response.addHeader("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() + 60000));

        filterChain.doFilter(request, response);
    }

    private String getIdentifier(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof Long) {
            Long userId = (Long) auth.getPrincipal();
            return "user:" + userId;
        }
        // Fallback – по IP
        return "ip:" + getClientIp(request);
    }

    private int getLimitForUri(String uri) {
        // Точное совпадение
        if (properties.getUrls().containsKey(uri)) {
            return properties.getUrls().get(uri);
        }
        // Паттерны
        for (Map.Entry<String, Integer> entry : properties.getUrls().entrySet()) {
            if (pathMatcher.match(entry.getKey(), uri)) {
                return entry.getValue();
            }
        }
        return properties.getDefaultLimit();
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
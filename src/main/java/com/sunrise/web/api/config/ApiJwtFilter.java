package com.sunrise.web.api.config;

import com.sunrise.helpclass.JwtUtil;
import com.sunrise.orchestrator.service.UserOrchestrator;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
public class ApiJwtFilter extends OncePerRequestFilter {

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final JwtUtil jwtUtil;
    private final UserOrchestrator userOrchestrator;
    
    @Value("${app.jwt.no-jwt-endpoints}")
    private final String[] excludedPaths;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (isPathExcluded(request.getServletPath())) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "MISSING_TOKEN");
            return;
        }

        String jwt;
        Long userId;
        Integer tokenVersion;

        try {
            jwt = authorizationHeader.substring(7);
            if (jwt.trim().isEmpty()) {
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "EMPTY_TOKEN");
                return;
            }

            userId = jwtUtil.extractUserId(jwt);
            tokenVersion = jwtUtil.extractJwtVersion(jwt);
        } catch (Exception e) {
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "INVALID_TOKEN");
            return;
        }

        if (userId == null || userId <= 0) {
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "INVALID_USER_ID");
            return;
        }

        if (!jwtUtil.validateToken(jwt)) {
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "TOKEN_VALIDATION_FAILED");
            return;
        }

        Optional<Integer> jwtVersion = userOrchestrator.getUserJwtVersion(userId);
        if (tokenVersion == null || jwtVersion.isEmpty() || !tokenVersion.equals(jwtVersion.get())) {
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "TOKEN_VERSION_MISMATCH");
            return;
        }

        try {
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userId, null, List.of());

            Map<String, Object> details = new HashMap<>();
            details.put("userId", userId);
            details.put("webAuthenticationDetails", new WebAuthenticationDetailsSource().buildDetails(request));
            auth.setDetails(details);

            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (Exception e) {
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "AUTHENTICATION_ERROR");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPathExcluded(String path) {
        if (excludedPaths == null || path == null) {
            return false;
        }

        for (String pattern : excludedPaths) {
            if (pattern == null) continue;
            
            // Поддержка паттернов типа /auth/**
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
            if (path.equals(pattern)) {
                return true;
            }
        }
        return false;
    }
    
    private void sendErrorResponse(HttpServletResponse response, int statusCode, String errorCode) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/text");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(errorCode);
        response.getWriter().flush();
    }
}
package com.sunrise.web.api.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.sunrise.helpclass.JwtUtil;
import com.sunrise.orchestrator.service.UserOrchestrator;

import java.util.Arrays;
import java.util.List;

@Slf4j
@EnableWebSecurity
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(ApiRateLimitProperties.class)
public class ApiSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, ApiJwtFilter jwtFilter, ApiRateLimitFilter rateLimitFilter) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Включаем CORS с настройками
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(rateLimitFilter, ApiJwtFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() { // Настройка доменов, которым можно обращаться к моему супер ПО
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*")); // Разрешаем все домены пока что
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public ApiJwtFilter apiJwtFilter(JwtUtil jwtUtil,
                                     UserOrchestrator userOrchestrator,
                                     @Value("${app.jwt.no-jwt-endpoints}") String[] excludedPaths) {
        return new ApiJwtFilter(jwtUtil, userOrchestrator, excludedPaths);
    }

    @Bean
    public ApiRateLimitFilter apiRateLimitFilter(ApiRateLimitProperties properties) {
        return new ApiRateLimitFilter(properties);
    }
}
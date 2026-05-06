package com.sunrise.helpclass;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

import java.nio.charset.StandardCharsets;

import java.util.Date;
import java.util.Map;

@Component
public class JwtUtil {
    private SecretKey secretKey;

    @Value("${app.jwt.secret}")
    private String secretString;

    @Value("${app.jwt.expiration}")
    public long expiration; // in seconds

    @PostConstruct
    public void init() {
        if (secretString == null || secretString.isEmpty()) {
            throw new IllegalStateException("JWT secret must be not empty or null");
        }
        byte[] secretBytes = secretString.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 bytes. Current length: " + secretBytes.length);
        }
        this.secretKey = Keys.hmacShaKeyFor(secretBytes);
    }

    public String generateToken(long userId, int jwtVersion) {
        return Jwts.builder()
                .claims(Map.of("userId", userId, "jwtVersion", jwtVersion))
                .subject(String.valueOf(userId))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration * 1000))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }
    public String generateTokenForActuator(String username, long expiration) {
        return Jwts.builder()
                .claims(Map.of("username", username, "access", "actuator"))
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration * 1000))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            return !getClaims(token).getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Date getTokenExpirationTime(String token) {
        return getClaims(token).getExpiration();
    }

    public Long extractUserId(String token) {
        return getClaims(token).get("userId", Long.class);
    }
    public Integer extractJwtVersion(String token) {
        return getClaims(token).get("jwtVersion", Integer.class);
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

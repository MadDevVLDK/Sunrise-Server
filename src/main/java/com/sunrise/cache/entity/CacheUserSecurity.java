package com.sunrise.cache.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class CacheUserSecurity {
    private long id;
    private String email;
    private String hashPassword;
    private int jwtVersion;
    private boolean isEnabled;
    private Instant deletedAt;
    private boolean isDeleted;
}

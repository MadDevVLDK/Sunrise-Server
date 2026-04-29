package com.sunrise.cache.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class CacheUserProfile {
    private long id;
    private String username;
    private String name;
    private Instant profileUpdatedAt;
    private Instant createdAt;
    private Instant deletedAt;
    private boolean isDeleted;

    private final Instant cachedAt = Instant.now();
}

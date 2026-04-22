package com.sunrise.entity.cache;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class CacheUserProfile {
    private long id;
    private String username;
    private String name;
    private LocalDateTime profileUpdatedAt;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;
    private boolean isDeleted;

    private final LocalDateTime cachedAt = LocalDateTime.now();

    public static CacheUserProfile copy(CacheUserProfile user) {
        if (user == null) return null;

        return new CacheUserProfile(
            user.getId(),
            user.getUsername(),
            user.getName(),
            user.getProfileUpdatedAt(),
            user.getCreatedAt(),
            user.getDeletedAt(),
            user.isDeleted()
        );
    }
}

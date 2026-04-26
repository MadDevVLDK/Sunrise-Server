package com.sunrise.cache.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class CacheUserSecurity {
    private long id;
    private String email;
    private String hashPassword;
    private int jwtVersion;
    private boolean isEnabled;
    private LocalDateTime deletedAt;
    private boolean isDeleted;

    public static CacheUserSecurity copy(CacheUserSecurity user) {
        if (user == null) return null;

        return new CacheUserSecurity(
            user.getId(),
            user.getEmail(),
            user.getHashPassword(),
            user.getJwtVersion(),
            user.isEnabled(),
            user.getDeletedAt(),
            user.isDeleted()
        );
    }
}

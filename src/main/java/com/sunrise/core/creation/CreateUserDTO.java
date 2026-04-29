package com.sunrise.core.creation;

import java.time.Instant;
@lombok.Getter
public class CreateUserDTO {
    private final long id;
    private final String username;
    private final String name;
    private final String email;
    private final String hashPassword;
    private final Instant lastLogin = null;
    private final Instant profileUpdatedAt;
    private final Instant createdAt;
    private final int jwtVersion = 1;
    private final boolean isEnabled = false;
    private final Instant deletedAt = null;
    private final boolean isDeleted = false;

    public CreateUserDTO(long id, String username, String name, String email, String hashPassword, Instant createdAt) {
        this.id = id;
        this.username = username;
        this.name = name;
        this.email = email;
        this.hashPassword = hashPassword;
        this.profileUpdatedAt = createdAt;
        this.createdAt = createdAt;
    }
}

package com.sunrise.service.creation;

import java.time.LocalDateTime;
@lombok.Getter
public class CreateUserDTO {
    private final long id;
    private final String username;
    private final String name;
    private final String email;
    private final String hashPassword;
    private final LocalDateTime lastLogin = null;
    private final LocalDateTime profileUpdatedAt;
    private final LocalDateTime createdAt;
    private final int jwtVersion = 1;
    private final boolean isEnabled = false;
    private final LocalDateTime deletedAt = null;
    private final boolean isDeleted = false;

    public CreateUserDTO(long id, String username, String name, String email, String hashPassword, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.name = name;
        this.email = email;
        this.hashPassword = hashPassword;
        this.profileUpdatedAt = createdAt;
        this.createdAt = createdAt;
    }
}

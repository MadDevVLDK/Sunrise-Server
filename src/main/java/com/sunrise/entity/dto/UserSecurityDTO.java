package com.sunrise.entity.dto;

import java.time.LocalDateTime;

@lombok.Setter
@lombok.Getter
@lombok.AllArgsConstructor
public class UserSecurityDTO {
    private long id;
    private String email;
    private String hashPassword;
    private int jwtVersion;
    private boolean isEnabled;
    private LocalDateTime deletedAt;
    private boolean isDeleted;
}

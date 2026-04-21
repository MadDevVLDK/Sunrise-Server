package com.sunrise.entity.dto;

import java.time.LocalDateTime;

@lombok.Setter
@lombok.Getter
@lombok.AllArgsConstructor
public class UserAvatarDTO {
    private long id;
    private String hash;
    private String previewHash;
    private boolean isPrimary;
    private LocalDateTime createdAt;
}
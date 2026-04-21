package com.sunrise.entity.dto;

import java.time.LocalDateTime;

@lombok.Getter
@lombok.Setter
@lombok.AllArgsConstructor
public class ChatAvatarDTO {
    private long id;
    private String hash;
    private String previewHash;
    private boolean isPrimary;
    private LocalDateTime createdAt;
}
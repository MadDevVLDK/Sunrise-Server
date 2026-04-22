package com.sunrise.entity.dto;

import java.time.LocalDateTime;

@lombok.Getter
@lombok.AllArgsConstructor
public class UserProfileLightDTO {
    private long id;
//    private UserAvatarDTO avatar;
    private String username;
    private String name;
    private LocalDateTime profileUpdatedAt;
    private LocalDateTime createdAt;
}

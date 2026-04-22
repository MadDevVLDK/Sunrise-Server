package com.sunrise.entity.dto;

import java.time.LocalDateTime;
import java.util.List;

@lombok.Setter
@lombok.Getter
@lombok.AllArgsConstructor
public class UserProfileFullDTO {
    private long id;
//    private List<UserAvatarDTO> avatars;
    private String username;
    private String name;
    private LocalDateTime profileUpdatedAt;
    private LocalDateTime createdAt;
}
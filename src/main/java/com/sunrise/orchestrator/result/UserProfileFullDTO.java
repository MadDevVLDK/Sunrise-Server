package com.sunrise.orchestrator.result;

import java.time.Instant;

@lombok.Getter
@lombok.AllArgsConstructor
public class UserProfileFullDTO {
    private long id;
//    private List<UserAvatarDTO> avatars;
    private String username;
    private String name;
    private Instant profileUpdatedAt;
    private Instant createdAt;
}
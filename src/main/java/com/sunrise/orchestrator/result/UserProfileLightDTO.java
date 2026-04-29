package com.sunrise.orchestrator.result;

import java.time.Instant;

@lombok.Getter
@lombok.AllArgsConstructor
public class UserProfileLightDTO {
    private long id;
//    private UserAvatarDTO avatar;
    private String username;
    private String name;
    private Instant profileUpdatedAt;
    private Instant createdAt;
}

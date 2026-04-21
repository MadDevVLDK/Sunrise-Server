package com.sunrise.entity.dto;

@lombok.Setter
@lombok.Getter
@lombok.AllArgsConstructor
public class ChatMemberProfileFullDTO {
    private UserProfileLightDTO userProfile;
    private ChatMemberProfileDTO memberProfile;
}

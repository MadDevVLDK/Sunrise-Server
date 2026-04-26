package com.sunrise.dataservice.result;

@lombok.Setter
@lombok.Getter
@lombok.AllArgsConstructor
public class ChatMemberProfileFullDTO {
    private UserProfileLightDTO userProfile;
    private ChatMemberProfileDTO memberProfile;
}

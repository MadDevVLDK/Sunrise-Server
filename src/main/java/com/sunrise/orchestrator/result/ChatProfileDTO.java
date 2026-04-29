package com.sunrise.orchestrator.result;

import java.time.Instant;

import com.sunrise.orchestrator.type.ChatType;

@lombok.Setter
@lombok.Getter
@lombok.AllArgsConstructor
public class ChatProfileDTO {
    private long id;
//    private ChatAvatarDTO avatar;
    private String name;
    private String description;
    private ChatType chatType;
    private ChatMemberFullDTO rights;
    private ChatMemberProfileFullDTO opponent; // Только для личных чатов
    private int membersCount;
    private UserMessageDTO lastMessage;
    private Long lastReadMessageId;
    private int unreadCount;
    private long seq;
    private Instant updatedAt;
    private Instant createdAt;
    private long createdBy;
}

package com.sunrise.entity.dto;

import com.sunrise.core.dataservice.type.ChatType;

import java.time.LocalDateTime;

@lombok.Setter
@lombok.Getter
@lombok.AllArgsConstructor
public class ChatProfileDTO {
    private long id;
//    private ChatAvatarDTO avatar;
    private String name;
    private String description;
    private ChatType chatType;
    private Long opponentId; // Только для личных чатов
    private int membersCount;
    private UserMessageDTO lastMessage;
    private int unreadCount;
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;
    private long createdBy;
}

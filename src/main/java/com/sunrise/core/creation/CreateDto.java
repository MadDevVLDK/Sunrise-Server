package com.sunrise.core.creation;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.sunrise.orchestrator.type.ChatType;
import com.sunrise.orchestrator.type.MessageType;
import com.sunrise.orchestrator.type.TokenType;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class CreateDto {
    
    @Getter
    public static class User {
        private final long id;
        private final String username;
        private final String name;
        private final String email;
        private final String hashPassword;
        private final Instant lastLogin = null;
        private final Instant profileUpdatedAt;
        private final Instant createdAt;
        private final int jwtVersion = 1;
        private final boolean isEnabled = false;
        private final Instant deletedAt = null;
        private final boolean isDeleted = false;

        public User(long id, String username, String name, String email, String hashPassword, Instant createdAt) {
            this.id = id;
            this.username = username;
            this.name = name;
            this.email = email;
            this.hashPassword = hashPassword;
            this.profileUpdatedAt = createdAt;
            this.createdAt = createdAt;
        }
    }


    @Getter 
    public static class GroupChat {
        private final long id;
        private final Long avatarFileId = null;
        private final Long avatarPreviewFileId = null;
        private final String name;
        private final String description;
        private final ChatType chatType = ChatType.GROUP;
        private final Long opponentId = null;
        private final int membersCount;
        private final Instant updatedAt;
        private final Instant createdAt;
        private final long createdBy;
        private final Instant deletedAt = null;
        private final boolean isDeleted = false;

        public GroupChat(long id, String name, String description, int membersCount, Instant createdAt, long createdBy) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.membersCount = membersCount;
            this.updatedAt = createdAt;
            this.createdAt = createdAt;
            this.createdBy = createdBy;
        }
    }


    @Getter
    public static class PersonalChat {
        private final long id;
        private final Long avatarFileId = null;
        private final Long avatarPreviewFileId = null;
        private final String name = null;
        private final String description = null;
        private final ChatType chatType = ChatType.PERSONAL;
        private final long opponentId;
        private final int membersCount = 2;
        private final Instant updatedAt;
        private final Instant createdAt;
        private final long createdBy;
        private final Instant deletedAt = null;
        private final boolean isDeleted = false;

        public PersonalChat(long id, Long opponentId, Instant createdAt, long createdBy) {
            this.id = id;
            this.opponentId = opponentId;
            this.updatedAt = createdAt;
            this.createdAt = createdAt;
            this.createdBy = createdBy;
        }
    }


    @Getter 
    public static class ChatMember {
        private final long chatId;
        private final long userId;
        private final String tag = null;
        private final Instant settingsUpdatedAt;
        private final Instant updatedAt;
        private final Instant joinedAt;
        private final boolean isPinned = false;
        private final boolean isAdmin;
        private final Instant deletedAt = null;
        private final boolean isDeleted = false;

        public ChatMember(long chatId, long userId, Instant joinedAt, boolean isAdmin) {
            this.chatId = chatId;
            this.userId = userId;
            this.settingsUpdatedAt = joinedAt;
            this.updatedAt = joinedAt;
            this.joinedAt = joinedAt;
            this.isAdmin = isAdmin;
        }
    }


    @Getter
    public static class Message {
        private final long id;
        private final long chatId;
        private final long senderId;
        private final MessageType messageType;
        private final String text;
        private final long readCount = 0L;
        private final Instant sentAt;
        private final Instant updatedAt;
        private final Instant deletedAt = null;
        private final boolean isDeleted = false;

        public Message(long id, long chatId, long senderId, MessageType messageType, String text, Instant sentAt) {
            this.id = id;
            this.chatId = chatId;
            this.senderId = senderId;
            this.messageType = messageType;
            this.text = text;
            this.sentAt = sentAt;
            this.updatedAt = sentAt;
        }
    }


    @Getter
    public static class VerificationToken {
        private final long id;
        private final long userId;
        private final String token;
        private final TokenType tokenType;
        private final Instant expiryDate;
        private final Instant createdAt;

        public VerificationToken(long id, long userId, String token, TokenType tokenType, Instant createdAt, int expireInHours) {
            this.id = id;
            this.token = token;
            this.userId = userId;
            this.expiryDate = createdAt.plus(expireInHours, ChronoUnit.HOURS);
            this.createdAt = createdAt;
            this.tokenType = tokenType;
        }
    }


    @Getter @AllArgsConstructor 
    public static class LoginHistory {
        private long id;
        private long userId;
        private String ipAddress;
        private String deviceInfo;
        private Instant loginAt;
    }
}

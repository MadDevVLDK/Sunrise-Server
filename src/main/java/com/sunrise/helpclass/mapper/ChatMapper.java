package com.sunrise.helpclass.mapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.sunrise.cache.entity.*;
import com.sunrise.core.creation.*;
import com.sunrise.db.entity.*;
import com.sunrise.db.result.*;
import com.sunrise.orchestrator.result.*;
import com.sunrise.orchestrator.type.ChatType;

public class ChatMapper {
    
    
    // ========== CHAT ==========

    public static CacheChat copy(CacheChat chat) {
        if (chat == null) return null;

        return new CacheChat(
            chat.getId(),
            chat.getName(),
            chat.getDescription(),
            chat.getChatType(),
            chat.getOpponentId(),
            chat.getMembersCount(),
            chat.getUpdatedAt(),
            chat.getCreatedAt(),
            chat.getCreatedBy(),
            chat.getDeletedAt(),
            chat.isDeleted()
        );
    }

    public static CacheChat toCache(CreateGroupChatDTO chat) {
        if (chat == null) return null;

        return new CacheChat(
            chat.getId(),
            chat.getName(),
            chat.getDescription(),
            chat.getChatType(),
            chat.getOpponentId(),
            chat.getMembersCount(),
            chat.getUpdatedAt(),
            chat.getCreatedAt(),
            chat.getCreatedBy(),
            chat.getDeletedAt(),
            chat.isDeleted()
        );
    }

    public static CacheChat toCache(CreatePersonalChatDTO chat) {
        if (chat == null) return null;

        return new CacheChat(
            chat.getId(),
            chat.getName(),
            chat.getDescription(),
            chat.getChatType(),
            chat.getOpponentId(),
            chat.getMembersCount(),
            chat.getUpdatedAt(),
            chat.getCreatedAt(),
            chat.getCreatedBy(),
            chat.getDeletedAt(),
            chat.isDeleted()
        );
    }

    public static CacheChat toCache(ChatProfileResult chat) {
        if (chat == null) return null;

        return new CacheChat(
            chat.getId(),
            chat.getName(),
            chat.getDescription(),
            ChatType.valueOf(chat.getChatType()),
            chat.getOpponentId(),
            chat.getMembersCount(),
            chat.getUpdatedAt(),
            chat.getCreatedAt(),
            chat.getCreatedBy(),
            chat.getDeletedAt(),
            chat.getIsDeleted()
        );
    }

    public static CacheChat toCache(UserChatResult chat) {
        if (chat == null) return null;

        return new CacheChat(
            chat.getId(),
            chat.getName(),
            chat.getDescription(),
            ChatType.valueOf(chat.getChatType()),
            chat.getOpponentId(),
            chat.getMembersCount(),
            chat.getUpdatedAt(),
            chat.getCreatedAt(),
            chat.getCreatedBy(),
            chat.getDeletedAt(),
            chat.getIsDeleted()
        );
    }

    public static List<CacheChat> toCaches(Collection<UserChatResult> items) {
        if (items == null) return Collections.emptyList();

        List<CacheChat> cached = new ArrayList<>(items.size());
        for (UserChatResult item : items) {
            cached.add(toCache(item));
        }
        return cached;
    }

    public static Chat toEntity(CreateGroupChatDTO chat) {
        if (chat == null) return null;

        return new Chat(
            chat.getId(),
            chat.getName(),
            chat.getDescription(),
            chat.getChatType(),
            chat.getOpponentId(),
            chat.getMembersCount(),
            chat.getUpdatedAt(),
            chat.getCreatedAt(),
            chat.getCreatedBy(),
            chat.getDeletedAt(),
            chat.isDeleted()
        );
    }

    public static Chat toEntity(CreatePersonalChatDTO chat) {
        if (chat == null) return null;

        return new Chat(
            chat.getId(),
            chat.getName(),
            chat.getDescription(),
            chat.getChatType(),
            chat.getOpponentId(),
            chat.getMembersCount(),
            chat.getUpdatedAt(),
            chat.getCreatedAt(),
            chat.getCreatedBy(),
            chat.getDeletedAt(),
            chat.isDeleted()
        );
    }

    public static ChatSecurityDTO toSecurityDTO(ChatProfileResult chat) {
        if (chat == null) return null;

        return new ChatSecurityDTO(
            chat.getId(),
            ChatType.valueOf(chat.getChatType()),
            chat.getMembersCount(),
            chat.getCreatedAt(),
            chat.getCreatedBy(),
            chat.getDeletedAt(),
            chat.getIsDeleted()
        );
    }

    public static ChatSecurityDTO toSecurityDTO(CacheChat chat) {
        if (chat == null) return null;

        return new ChatSecurityDTO(
            chat.getId(),
            chat.getChatType(),
            chat.getMembersCount(),
            chat.getCreatedAt(),
            chat.getCreatedBy(),
            chat.getDeletedAt(),
            chat.isDeleted()
        );
    }

    public static ChatMetaDTO toMetaDTO(ChatMetaResult chat) {
        if (chat == null) return null;

        return new ChatMetaDTO(
            chat.getChatId(),
            chat.getIsPinned(),
            chat.getLastMsgId(),
            chat.getUnreadCount(),
            chat.getSeq()
        );
    }

    public static ChatProfileDTO toProfileDTO(UserChatResult chat, long userId) {
        if (chat == null) return null;

        return new ChatProfileDTO(
            chat.getId(),
            chat.getName() != null ? chat.getName() : chat.getOpponentName(),
            chat.getDescription(),
            ChatType.valueOf(chat.getChatType()),
            ChatMemberMapper.toSelfProfileDTO(chat, userId),
            ChatMemberMapper.toOpponentProfileFullDTO(chat),
            chat.getMembersCount(),
            MessageMapper.toUserDTO(chat),
            chat.getLastReadMessageId(),
            chat.getUnreadCount(),
            chat.getSeq(),
            chat.getUpdatedAt(),
            chat.getCreatedAt(),
            chat.getCreatedBy()
        );
    }

    public static List<ChatProfileDTO> toProfileDTOs(Collection<UserChatResult> chats, long userId) {
        if (chats == null) return null;

        List<ChatProfileDTO> resultMap = new LinkedList<>();
        for (UserChatResult chat : chats) {
            resultMap.add(toProfileDTO(chat, userId));
        }
        return resultMap;
    }

    // ========== CHAT AVATAR (закомментировано) ==========
    // public static ChatAvatarDTO toChatAvatarDTO(UserChatResult chat) { ... }
}

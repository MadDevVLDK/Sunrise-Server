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

    public static Cache.Chat copy(Cache.Chat chat) {
        if (chat == null) return null;

        return new Cache.Chat(
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

    public static Cache.Chat toCache(CreateDto.GroupChat chat) {
        if (chat == null) return null;

        return new Cache.Chat(
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

    public static Cache.Chat toCache(CreateDto.PersonalChat chat) {
        if (chat == null) return null;

        return new Cache.Chat(
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

    public static Cache.Chat toCache(ChatProfileResult chat) {
        if (chat == null) return null;

        return new Cache.Chat(
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

    public static Cache.Chat toCache(UserChatResult chat) {
        if (chat == null) return null;

        return new Cache.Chat(
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

    public static List<Cache.Chat> toCaches(Collection<UserChatResult> items) {
        if (items == null) return Collections.emptyList();

        List<Cache.Chat> cached = new ArrayList<>(items.size());
        for (UserChatResult item : items) {
            cached.add(toCache(item));
        }
        return cached;
    }

    public static Chat toEntity(CreateDto.GroupChat chat) {
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

    public static Chat toEntity(CreateDto.PersonalChat chat) {
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

    public static Dto.ChatSecurity toSecurityDTO(ChatProfileResult chat) {
        if (chat == null) return null;

        return new Dto.ChatSecurity(
            chat.getId(),
            ChatType.valueOf(chat.getChatType()),
            chat.getMembersCount(),
            chat.getCreatedAt(),
            chat.getCreatedBy(),
            chat.getDeletedAt(),
            chat.getIsDeleted()
        );
    }

    public static Dto.ChatSecurity toSecurityDTO(Cache.Chat chat) {
        if (chat == null) return null;

        return new Dto.ChatSecurity(
            chat.getId(),
            chat.getChatType(),
            chat.getMembersCount(),
            chat.getCreatedAt(),
            chat.getCreatedBy(),
            chat.getDeletedAt(),
            chat.isDeleted()
        );
    }

    public static Dto.ChatMeta toMetaDTO(ChatMetaResult chat) {
        if (chat == null) return null;

        return new Dto.ChatMeta(
            chat.getChatId(),
            chat.getIsPinned(),
            chat.getLastMsgId(),
            chat.getUnreadCount()
        );
    }

    public static Dto.ChatProfile toProfileDTO(UserChatResult chat, long userId) {
        if (chat == null) return null;

        return new Dto.ChatProfile(
            chat.getId(),
            chat.getName() != null ? chat.getName() : chat.getOpponentName(),
            chat.getDescription(),
            ChatType.valueOf(chat.getChatType()),
            ChatMemberMapper.toSelfProfileDTO(chat, userId),
            ChatMemberMapper.toOpponentProfileFullDTO(chat),
            chat.getMembersCount(),
            MessageMapper.toUserDTO(chat),
            chat.getLastReadMessageIdByMe(),
            chat.getLastReadMessageIdByAnyone(),
            chat.getUnreadCount(),
            chat.getUpdatedAt(),
            chat.getCreatedAt(),
            chat.getCreatedBy()
        );
    }

    public static List<Dto.ChatProfile> toProfileDTOs(Collection<UserChatResult> chats, long userId) {
        if (chats == null) return null;

        List<Dto.ChatProfile> resultMap = new LinkedList<>();
        for (UserChatResult chat : chats) {
            resultMap.add(toProfileDTO(chat, userId));
        }
        return resultMap;
    }

    public static Dto.ChatStatsResult toChatStatsDTO(ChatStatsResult stats) {
        if (stats == null) return null;

        return new Dto.ChatStatsResult(
            stats.getTotalMessages(),
            stats.getDeletedForAll(),
            stats.getCanDeleteForAll()
        );
    }

    // ========== CHAT AVATAR (закомментировано) ==========
    // public static ChatAvatarDTO toChatAvatarDTO(UserChatResult chat) { ... }
}

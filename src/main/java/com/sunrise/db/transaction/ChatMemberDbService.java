package com.sunrise.db.transaction;

import com.sunrise.db.DBService;
import com.sunrise.db.entity.ChatMember;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatMemberDbService {

    private final DBService dbService;

    @Transactional
    public void upsertChatMember(ChatMember chatMember) {
        dbService.upsertChatMember(chatMember);
    }

    @Transactional
    public void upsertChatMembers(long chatId, Long[] memberIds, LocalDateTime joinedAt) {
        dbService.upsertChatMembers(chatId, memberIds, joinedAt);
    }

    @Transactional
    public int updateChatMemberInfo(long chatId, long userId, String tag, LocalDateTime updatedAt) {
        return dbService.updateChatMemberInfo(chatId, userId, tag, updatedAt);
    }

    @Transactional
    public int updateChatMemberAdminRights(long chatId, long userId, boolean isAdmin, LocalDateTime updatedAt) {
        return dbService.updateChatMemberAdminRights(chatId, userId, isAdmin, updatedAt);
    }

    @Transactional
    public int updateChatMemberSettings(long chatId, long userId, boolean isPinned, LocalDateTime updatedAt) {
        return dbService.updateChatMemberSettings(chatId, userId, isPinned, updatedAt);
    }

    @Transactional
    public boolean removeChatMember(long userId, long chatId, LocalDateTime updatedAt) {
        return dbService.removeChatMember(userId, chatId, updatedAt);
    }

    @Transactional(readOnly = true)
    public Optional<ChatMember> getChatMember(long chatId, long userId) {
        return dbService.getChatMember(chatId, userId);
    }

    @Transactional(readOnly = true)
    public Optional<ChatMember> getActiveChatMember(long chatId, long userId) {
        return dbService.getActiveChatMember(chatId, userId);
    }

    @Transactional(readOnly = true)
    public List<ChatMember> getActiveChatMembersByIds(long chatId, List<Long> missingIds) {
        return dbService.getActiveChatMembersByIds(chatId, missingIds);
    }

    @Transactional(readOnly = true)
    public List<Long> getChatMemberIdsPage(long chatId, Long cursor, int limit) {
        return dbService.getChatMemberIdsPage(chatId, cursor, limit);
    }
}
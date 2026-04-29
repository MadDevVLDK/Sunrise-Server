package com.sunrise.db.service;

import com.sunrise.db.entity.ChatMember;
import com.sunrise.db.entity.ChatMemberId;
import com.sunrise.db.repository.ChatMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMemberDbService {

    private final ChatMemberRepository chatMemberRepository;

    @Transactional
    public boolean saveOrRestore(ChatMember chatMember) {
        log.debug("[🗄️] 👤 Upserting chat member: chatId={}, userId={}, isAdmin={}", 
            chatMember.getChatId(), chatMember.getUserId(), chatMember.isAdmin());
        boolean result = chatMemberRepository.saveOrRestore(
            chatMember.getChatId(), 
            chatMember.getUserId(), 
            chatMember.isAdmin(), 
            chatMember.getJoinedAt()
        );
        log.debug("[🗄️] 👤🚪 Chat member upserted: {}", result);
        return result;
    }

    @Transactional
    public Long[] saveOrRestoreBatch(long chatId, Long[] memberIds, Instant joinedAt) {
        log.debug("[🗄️] 👥 Batch upserting {} chat members in chat {}", memberIds.length, chatId);
        Long[] chatMembersIds = chatMemberRepository.saveOrRestoreBatch(chatId, memberIds, joinedAt);
        log.debug("[🗄️] 👤🚪 Chat members upserted: {}", chatMembersIds.length);
        return chatMembersIds;
    }

    @Transactional
    public int updateProfile(long chatId, long userId, String tag, Instant updatedAt) {
        log.debug("[🗄️] 👤📝 Updating chat member info: chatId={}, userId={}, tag={}", chatId, userId, tag);
        int result = chatMemberRepository.updateProfile(chatId, userId, tag, updatedAt);
        log.debug("[🗄️] 👤📝 Chat member info updated: affectedRows={}", result);
        return result;
    }

    @Transactional
    public int updateAdminRights(long chatId, long userId, boolean isAdmin, Instant updatedAt) {
        log.debug("[🗄️] 👤👑 Updating chat member admin rights: chatId={}, userId={}, isAdmin={}", chatId, userId, isAdmin);
        int result = chatMemberRepository.updateAdminRights(chatId, userId, isAdmin, updatedAt);
        log.debug("[🗄️] 👤👑 Chat member admin rights updated: affectedRows={}", result);
        return result;
    }

    @Transactional
    public int updateSettings(long chatId, long userId, boolean isPinned, Instant updatedAt) {
        log.debug("[🗄️] 👤⚙️ Updating chat member settings: chatId={}, userId={}, isPinned={}", chatId, userId, isPinned);
        int result = chatMemberRepository.updateSettings(chatId, userId, isPinned, updatedAt);
        log.debug("[🗄️] 👤⚙️ Chat member settings updated: affectedRows={}", result);
        return result;
    }

    @Transactional
    public boolean remove(long userId, long chatId, Instant updatedAt) {
        log.debug("[🗄️] 👤🚪 Removing chat member: userId={}, chatId={}", userId, chatId);
        boolean result = chatMemberRepository.remove(chatId, userId, updatedAt);
        log.debug("[🗄️] 👤🚪 Chat member removed: {}", result);
        return result;
    }

    @Transactional(readOnly = true)
    public Optional<ChatMember> get(long chatId, long userId) {
        log.debug("[🗄️] 👤🔍 Getting chat member: chatId={}, userId={}", chatId, userId);
        return chatMemberRepository.findById(new ChatMemberId(chatId, userId));
    }

    @Transactional(readOnly = true)
    public Optional<ChatMember> getActive(long chatId, long userId) {
        log.debug("[🗄️] 👤🔍 Getting active chat member: chatId={}, userId={}", chatId, userId);
        return chatMemberRepository.getActive(chatId, userId);
    }

    @Transactional(readOnly = true)
    public List<ChatMember> getActiveBatch(long chatId, List<Long> missingIds) {
        log.debug("[🗄️] 👤🔍 Getting active chat members by {} ids in chat {}", missingIds.size(), chatId);
        return chatMemberRepository.getActiveBatch(chatId, missingIds);
    }

    @Transactional(readOnly = true)
    public List<ChatMember> getBatchByChatAndIds(long chatId, List<Long> userIds) {
        return chatMemberRepository.getBatch(chatId, userIds);
    }

    @Transactional(readOnly = true)
    public List<Long> getIdsPage(long chatId, Long cursor, int limit) {
        log.debug("[🗄️] 👤📄 Getting chat member ID page: chatId={}, cursor={}, limit={}", chatId, cursor, limit);
        return chatMemberRepository.getIdsPage(chatId, cursor, PageRequest.of(0, limit));
    }
}
package com.sunrise.core.service;

import com.sunrise.core.creation.CreateDto;
import com.sunrise.orchestrator.DataValidator;
import com.sunrise.orchestrator.result.Dto.*;
import com.sunrise.orchestrator.service.ChatMemberOrchestrator;
import com.sunrise.orchestrator.service.ChatOrchestrator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMemberService {

    private final ChatMemberOrchestrator chatMemberOrchestrator;
    private final ChatOrchestrator chatOrchestrator;

    private final DataValidator validator;

    @Transactional
    public void addOrRestoreChatMember(long chatId, long inviterId, long opponentId) {
        validator.validateCanAddChatMember(chatId, inviterId, opponentId);

        CreateDto.ChatMember member = new CreateDto.ChatMember(
            chatId, opponentId, Instant.now(), false
        );

        boolean isUpdated = chatMemberOrchestrator.saveOrRestore(member);
        if (isUpdated) {
            log.info("[🔧] ✅ User {} added user {} to group chat {}", inviterId, opponentId, chatId);
        }
    }

    @Transactional
    public void addOrRestoreChatMembers(long chatId, long inviterId, @NonNull Set<Long> usersToAdd) {
        validator.validateCanAddChatMembers(chatId, inviterId, usersToAdd);

        Instant createdAt = Instant.now();

        List<CreateDto.ChatMember> members = new ArrayList<>(usersToAdd.size() + 1);
        for (long userId : usersToAdd) {
            members.add(new CreateDto.ChatMember(chatId, userId, createdAt, false));
        }

        Long[] addedIds = chatMemberOrchestrator.saveOrRestoreBatch(chatId, members);
        if (addedIds.length > 0) {
            log.info("[🔧] ✅ User {} added {} users to chat {}", inviterId, addedIds.length, chatId);
        }
    }

    @Transactional
    public void updateChatMemberInfo(long chatId, long adminId, long userToUpdateId, @NonNull String tag) {
        validator.validateCanUpdateChatMemberInfo(chatId, adminId, userToUpdateId);

        Instant updatedAt = Instant.now();
        boolean isUpdated = chatMemberOrchestrator.updateProfile(chatId, adminId, tag, updatedAt);
        if (isUpdated) {
            log.info("[🔧] ✅ Updated member info for user {} by admin {} in chat {}", userToUpdateId, adminId, chatId);
        }
    }

    @Transactional
    public void updateChatMemberAdminRight(long chatId, long adminId, long userToUpdateId, boolean isAdmin) {
        validator.validateCanUpdateChatMemberRights(chatId, adminId, userToUpdateId);

        Instant updatedAt = Instant.now();
        boolean isUpdated = chatMemberOrchestrator.updateAdminRights(chatId, userToUpdateId, isAdmin, updatedAt);
        if (isUpdated) {
            log.info("[🔧] ✅ Updated admin rights for user {} by admin {} in group chat {}", userToUpdateId, adminId, chatId);
        }
    }

    @Transactional
    public void updateSelfChatSettings(long chatId, long userId, boolean isPinned) {
        validator.validateActiveChatMemberInActiveChat(chatId, userId);

        Instant updatedAt = Instant.now();
        boolean isUpdated = chatMemberOrchestrator.updateSettings(chatId, userId, isPinned, updatedAt);
        if (isUpdated) {
            log.info("[🔧] ✅ Updated self settings for user {} in chat {}", userId, chatId);
        }
    }

    @Transactional
    public void kickChatMember(long chatId, long adminId, long userToKickId) {
        validator.validateCanKickChatMember(chatId, adminId, userToKickId);

        Instant updatedAt = Instant.now();
        boolean isUpdated = chatMemberOrchestrator.remove(chatId, userToKickId, updatedAt);
        if (isUpdated) {
            log.info("[🔧] ✅ User {} kicked from chat {} by user {}", userToKickId, chatId, adminId);
        }
    }

    @Transactional
    public void leaveChat(long chatId, long userId) {
        ChatSecurity chat = validator.validateActiveUserInActiveChatAndGetChat(chatId, userId);
        Instant updatedAt = Instant.now();

        if (chat.chatType().isPersonal()) {
            if (chat.membersCount() > 1) {
                boolean isUpdated = chatMemberOrchestrator.remove(chatId, userId, updatedAt);
                if (isUpdated) {
                    log.info("[🔧] ✅ User {} left group chat {}", userId, chatId);
                }
            } else {
                boolean isUpdated = chatOrchestrator.delete(chatId, updatedAt);
                if (isUpdated) {
                    log.info("[🔧] ✅ Last admin {} left group chat {}, chat deleted", userId, chatId);
                }
            }
        } else {
            boolean isUpdated = chatOrchestrator.delete(chatId, updatedAt);
            if (isUpdated) {
                log.info("[🔧] ✅ User {} deleted personal chat {}", userId, chatId);
            }
        }
    }

    @Transactional(readOnly = true)
    public ChatMembersPage getChatMemberPage(long chatId, long userId, Long cursor, int limit) {
        validator.validateActiveChatMemberInActiveChat(chatId, userId);

        ChatMembersPage chatMembers = chatMemberOrchestrator.getPage(chatId, cursor, limit);
        log.debug("[🔧] ✅ User {} got {} members of chat {}", userId, chatMembers.chatMembers().size(), chatId);
        return chatMembers;
    }

    @Transactional(readOnly = true)
    public List<ChatMemberProfile> getChatMemberByIds(long chatId, long userId, Set<Long> userIds) {
        validator.validateActiveChatMemberInActiveChat(chatId, userId);

        List<ChatMemberProfile> chatMembers = chatMemberOrchestrator.getProfilesByIds(chatId, userIds);
        log.debug("[🔧] ✅ User {} got {} members by ids of chat {}", userId, userIds.size(), chatId);
        return chatMembers;
    }
}
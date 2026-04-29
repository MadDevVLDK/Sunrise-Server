package com.sunrise.core.service;

import com.sunrise.core.creation.CreateChatMemberDTO;
import com.sunrise.core.result.ResultNoArgs;
import com.sunrise.core.result.ResultOneArg;
import com.sunrise.notifier.WebSocketNotifier;
import com.sunrise.orchestrator.DataValidator;
import com.sunrise.orchestrator.result.ChatMemberProfileDTO;
import com.sunrise.orchestrator.result.ChatMembersPageDTO;
import com.sunrise.orchestrator.result.ChatSecurityDTO;
import com.sunrise.orchestrator.service.ChatMemberOrchestrator;
import com.sunrise.orchestrator.service.ChatOrchestrator;
import com.sunrise.helpclass.ValidationException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Service
public class ChatMemberService {

    private final ChatMemberOrchestrator chatMemberOrchestrator;
    private final ChatOrchestrator chatOrchestrator;

    private final DataValidator validator;

    private final WebSocketNotifier wsNotify;


    @Transactional
    public ResultNoArgs addOrRestoreChatMember(long chatId, long inviterId, long opponentId) {
        try {
            validator.validateCanAddChatMember(chatId, inviterId, opponentId);

            CreateChatMemberDTO member = new CreateChatMemberDTO(chatId, opponentId, Instant.now(), false);

            boolean changed = chatMemberOrchestrator.saveOrRestore(member);
            if (changed) {
                // уведомить всех надо об этом
                wsNotify.notifyChatMemberNew(member);
                log.info("[🔧] ✅ User {} added user {} to group chat {}", inviterId, opponentId, chatId);
            } else {
                log.debug("[🔧] User {} was already active in chat {}", opponentId, chatId);
            }
            return ResultNoArgs.success();
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to add member to chat {}: {}", chatId, e.getMessage());
            return ResultNoArgs.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error adding member to chat {}: {}", chatId, e.getMessage());
            return ResultNoArgs.error("AddGroupMember failed due to server error");
        }
    }

    @Transactional
    public ResultNoArgs addOrRestoreChatMembers(long chatId, long inviterId, @NonNull Set<Long> usersToAdd) {
        try {
            validator.validateCanAddChatMembers(chatId, inviterId, usersToAdd);

            Instant createdAt = Instant.now();

            List<CreateChatMemberDTO> members = new ArrayList<>(usersToAdd.size() + 1);
            for (long userId : usersToAdd){
                members.add(new CreateChatMemberDTO(chatId, userId, createdAt, false));
            }

            Long[] addedIds = chatMemberOrchestrator.saveOrRestoreBatch(chatId, members);
            if (addedIds.length > 0) {
                List<CreateChatMemberDTO> addedMembers = members.stream()
                    .filter(m -> Arrays.asList(addedIds).contains(m.getUserId())).toList();

                 // уведомить всех надо об этом
                wsNotify.notifyChatMembersNew(addedMembers);
                log.info("[🔧] ✅ User {} added {} users to chat {}", inviterId, addedIds.length, chatId);
            } else {
                log.debug("[🔧] No new members were added to chat {}", chatId);
            }
            return ResultNoArgs.success();
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to add members to chat {}: {}", chatId, e.getMessage());
            return ResultNoArgs.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error adding members to chat {}: {}", chatId, e.getMessage());
            return ResultNoArgs.error("AddGroupMember failed due to server error");
        }
    }

    @Transactional
    public ResultNoArgs updateChatMemberInfo(long chatId, long adminId, long userToUpdateId, @NonNull String tag) {
        try {
            validator.validateCanUpdateChatMemberInfo(chatId, adminId, userToUpdateId);

            Instant updatedAt = Instant.now();
            boolean changed = chatMemberOrchestrator.updateProfile(chatId, adminId, tag, updatedAt);
            if (changed) {
                // уведомить всех надо об этом
                wsNotify.notifyChatMemberInfoUpdated(chatId, adminId, tag, updatedAt);
                log.info("[🔧] ✅ Updated member info for user {} by admin {} in chat {}", userToUpdateId, adminId, chatId);
            }
            return ResultNoArgs.success();
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to update member info for user {} by admin {} in chat {}: {}", userToUpdateId, adminId, chatId, e.getMessage());
            return ResultNoArgs.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error updating member info for user {} by admin {} in chat {}: {}", userToUpdateId, adminId, chatId, e.getMessage());
            return ResultNoArgs.error("updateChatMemberInfo failed due to server error");
        }
    }

    @Transactional
    public ResultNoArgs updateChatMemberAdminRight(long chatId, long adminId, long userToUpdateId, boolean isAdmin) {
        try {
            validator.validateCanUpdateChatMemberRights(chatId, adminId, userToUpdateId);

            Instant updatedAt = Instant.now();
            boolean changed = chatMemberOrchestrator.updateAdminRights(chatId, userToUpdateId, isAdmin, updatedAt);
            if (changed) {
                // уведомить всех надо об этом
                wsNotify.notifyChatMemberAdminRightsUpdated(chatId, userToUpdateId, isAdmin, updatedAt);
                log.info("[🔧] ✅ Updated admin rights for user {} by admin {} in group chat {}", userToUpdateId, adminId, chatId);
            }            
            return ResultNoArgs.success();
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to update admin rights for user {} by admin {} in group chat {}: {}", userToUpdateId, adminId, chatId, e.getMessage());
            return ResultNoArgs.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error updating admin rights for user {} by admin {} in group chat {}: {}", userToUpdateId, adminId, chatId, e.getMessage());
            return ResultNoArgs.error("updateChatMemberAdminRight failed due to server error");
        }
    }

    @Transactional
    public ResultNoArgs updateSelfChatSettings(long chatId, long userId, boolean isPinned) {
        try {
            validator.validateActiveChatMemberInActiveChat(chatId, userId);

            Instant updatedAt = Instant.now();
            boolean changed = chatMemberOrchestrator.updateSettings(chatId, userId, isPinned, updatedAt);
            if (changed) {
                // уведомить всех надо об этом
                wsNotify.notifySelfChatSettingsUpdated(chatId, userId, isPinned, updatedAt);
                log.info("[🔧] ✅ Updated self settings for user {} in chat {}", userId, chatId);
            }
            return ResultNoArgs.success();
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to update member info for user {} in chat {}: {}", userId, chatId, e.getMessage());
            return ResultNoArgs.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error updating member info for user {} in chat {}: {}", userId, chatId, e.getMessage());
            return ResultNoArgs.error("updateChatMemberInfo failed due to server error");
        }
    }

    @Transactional
    public ResultNoArgs kickChatMember(long chatId, long adminId, long userToKickId) {
        try {
            validator.validateCanKickChatMember(chatId, adminId, userToKickId);

            Instant updatedAt = Instant.now();
            boolean removed = chatMemberOrchestrator.remove(chatId, userToKickId, updatedAt);
            if (removed) {
                // уведомить всех надо об этом
                wsNotify.notifyChatMemberDeleted(chatId, userToKickId, updatedAt);
                log.info("[🔧] ✅ User {} kicked from chat {} by user {}", userToKickId, chatId, adminId);
            }
            return ResultNoArgs.success();
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to kick user {} from chat {}: {}", userToKickId, chatId, e.getMessage());
            return ResultNoArgs.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error kicking user {} from chat {}: {}", userToKickId, chatId, e.getMessage());
            return ResultNoArgs.error("kickChatMember failed due to server error");
        }
    }

    @Transactional
    public ResultNoArgs leaveChat(long chatId, long userId) {
        try {
            ChatSecurityDTO chat = validator.validateActiveUserInActiveChatAndGetChat(chatId, userId);
            Instant updatedAt = Instant.now();
            if (chat.getChatType().isPersonal()) {
                if (chat.getMembersCount() > 1) {
                    // Удаляем пользователя из чата
                    boolean removed = chatMemberOrchestrator.remove(chatId, userId, updatedAt);
                    if (removed) {
                        // уведомить всех надо об этом
                        wsNotify.notifyChatMemberDeleted(chatId, userId, updatedAt);
                        log.info("[🔧] ✅ User {} left group chat {}", userId, chatId);
                    }
                } else {
                    // Удаляем чат
                    boolean deleted = chatOrchestrator.delete(chatId, updatedAt);
                    if (deleted) {
                        // уведомить всех надо об этом
                        wsNotify.notifyChatDeleted(chatId, updatedAt);
                        log.info("[🔧] ✅ Last admin {} left group chat {}, chat deleted", userId, chatId);
                    }                    
                }
            } else {
                // Удаляем личный чат
                boolean deleted = chatOrchestrator.delete(chatId, updatedAt);
                if (deleted) {
                    // уведомить всех надо об этом
                    wsNotify.notifyChatDeleted(chatId, updatedAt);
                    log.info("[🔧] ✅ User {} deleted personal chat {}", userId, chatId);
                }
            }
            return ResultNoArgs.success();
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to leave chat {}: {}", chatId, e.getMessage());
            return ResultNoArgs.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error leaving chat {}: {}", chatId, e.getMessage());
            return ResultNoArgs.error("LeaveChat failed due to server error");
        }
    }

    public ResultOneArg<ChatMembersPageDTO> getChatMemberPage(long chatId, long userId, Long cursor, int limit) {
        try {
            validator.validateActiveChatMemberInActiveChat(chatId, userId);

            ChatMembersPageDTO chatMembers = chatMemberOrchestrator.getPage(chatId, cursor, limit);

            log.debug("[🔧] ✅ User {} got {} members of chat {}", userId, chatMembers.chatMembers().size(), chatId);
            return ResultOneArg.success(chatMembers);
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to get chat {} members: {}", chatId, e.getMessage());
            return ResultOneArg.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error getting chat {} members: {}", chatId, e.getMessage());
            return ResultOneArg.error("getChatMembers failed due to server error");
        }
    }
    public ResultOneArg<List<ChatMemberProfileDTO>> getChatMemberByIds(long chatId, long userId, Set<Long> userIds) {
        try {
            validator.validateActiveChatMemberInActiveChat(chatId, userId);

            List<ChatMemberProfileDTO> chatMembers = chatMemberOrchestrator.getProfilesByIds(chatId, userIds);

            log.debug("[🔧] ✅ User {} got {} members by ids of chat {}", userId, userIds.size(), chatId);
            return ResultOneArg.success(chatMembers);
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to get chat {} members by ids: {}", chatId, e.getMessage());
            return ResultOneArg.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error getting chat {} members by ids: {}", chatId, e.getMessage());
            return ResultOneArg.error("getChatMemberByIds failed due to server error");
        }
    }
}
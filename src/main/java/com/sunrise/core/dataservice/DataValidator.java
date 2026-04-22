package com.sunrise.core.dataservice;

import com.sunrise.core.dataservice.type.ChatType;
import com.sunrise.entity.dto.ChatSecurityDTO;
import com.sunrise.helpclass.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

@RequiredArgsConstructor
@Component
public class DataValidator {

    private final DataOrchestrator dataOrchestrator;


    // ========== BASE METHODS ==========

    public void validateActiveUser(long userId) {
        if (!dataOrchestrator.isActiveUser(userId)) {
            throw new ValidationException("User is not active -> " + userId);
        }
    }
    public void validateActiveUsers(long userId, long otherUserId) {
        if (userId == otherUserId) {
            throw new ValidationException("User equals to other user -> " + userId);
        }

        validateActiveUser(userId);
        validateActiveUser(otherUserId);
    }
    private void validateActiveUsers(long userId, Set<Long> userIds) {
        if (userIds.contains(userId)) {
            throw new ValidationException("User in users list -> " + userId);
        }

        validateActiveUser(userId);
        userIds.forEach(this::validateActiveUser);
    }
    private void validateActiveChat(long chatId) {
        if (!dataOrchestrator.isActiveChat(chatId)) {
            throw new ValidationException("Chat does not exist or is deleted -> " + chatId);
        }
    }
    private void validateActiveChatMember(long chatId, long userId) {
        if (!dataOrchestrator.hasActiveChatMember(chatId, userId)) {
            throw new ValidationException("User " + userId + " is not a member of the chat " + chatId);
        }
    }
    private void validateNotActiveChatMember(long chatId, long userId) {
        if (dataOrchestrator.hasActiveChatMember(chatId, userId)) {
            throw new ValidationException("User " + userId + " is already a member of the chat " + chatId);
        }
    }

    private void validateActiveGroupChat(long chatId) {
        Optional<Boolean> isGroup = dataOrchestrator.isActiveGroupChat(chatId);
        if (isGroup.isEmpty()) {
            throw new ValidationException("Chat does not exist or is deleted: " + chatId);
        }
        if (!isGroup.get()) {
            throw new ValidationException("Chat is a personal chat: " + chatId);
        }
    }
    private ChatSecurityDTO validateActiveChatAndGet(long chatId) {
        Optional<ChatSecurityDTO> chatOpt = dataOrchestrator.getActiveChat(chatId);
        if (chatOpt.isEmpty()) {
            throw new ValidationException("Chat does not exist or is deleted -> " + chatId);
        }
        return chatOpt.get();
    }
    private void validateActiveChatMemberIsAdmin(long chatId, long userId){
        Optional<Boolean> isAdmin = dataOrchestrator.isActiveAdminInActiveChat(chatId, userId);
        if (isAdmin.isEmpty()) {
            throw new ValidationException("User is not a member of this chat -> " + userId);
        }
        if (!isAdmin.get()) {
            throw new ValidationException("User is not group admin -> " + userId);
        }
    }

    public void validateActiveMessageInChat(long chatId, long messageId) {
        if (!dataOrchestrator.isActiveMessageInChat(chatId, messageId)){
            throw new ValidationException("Message does not exist or is deleted -> " + messageId);
        }
    }
    private void validateActiveMessageInChatAndIsSender(long chatId, long userId, long messageId) {
        if (!dataOrchestrator.isActiveMessageInChatAndIsSender(chatId, userId, messageId)){
            throw new ValidationException("Message does not exist or is deleted -> " + messageId);
        }
    }


    // ========== COMPLEX METHODS ==========

    public void validateCanCreateGroupChat(ChatType chatType, long userId, Set<Long> userIds) {
        if (chatType.isPersonal()) {
            throw new ValidationException("Cannot create group if chatType is personal");
        }
        if (!chatType.isMembersInBound(userIds.size() + 1)) {
            throw new ValidationException("Members not in bound of chatType --> " + chatType);
        }

        validateActiveUsers(userId, userIds);
    }
    public void validateCanUpdateChatInfo(long chatId, long userId) {
        validateActiveUser(userId);
        ChatSecurityDTO chat = validateActiveChatAndGet(chatId);
        if (chat.getChatType().isPersonal()) {
            throw new ValidationException("Chat info is not changeable for private chat");
        }
        validateActiveChatMemberIsAdmin(chatId, userId);
    }
    public void validateCanDeleteChat(long chatId, long userId) {
        validateActiveChatMemberInActiveChat(chatId, userId);
        validateActiveChatMemberIsAdmin(chatId, userId);
    }

    public void validateCanAddChatMembers(long chatId, long userId, Set<Long> newUserIds) {
        validateActiveUsers(userId, newUserIds);
        validateActiveGroupChat(chatId);
        validateActiveChatMemberIsAdmin(chatId, userId);
        newUserIds.forEach(id -> validateNotActiveChatMember(chatId, id));
    }
    public void validateCanAddChatMember(long chatId, long userId, long otherUserId) {
        validateActiveUsers(userId, otherUserId);
        validateActiveGroupChat(chatId);
        validateActiveChatMemberIsAdmin(chatId, userId);
        validateNotActiveChatMember(chatId, otherUserId);
    }
    public void validateCanUpdateChatMemberInfo(long chatId, long adminId, long otherUserId) {
        validateActiveUsers(adminId, otherUserId);
        validateActiveChat(chatId);
        validateActiveChatMemberIsAdmin(chatId, adminId);
    }
    public void validateCanUpdateChatMemberRights(long chatId, long adminId, long otherUserId) {
        validateActiveUsers(adminId, otherUserId);
        validateActiveChat(chatId);
        validateActiveChatMemberIsAdmin(chatId, adminId);
        validateActiveChatMember(chatId, otherUserId);
    }
    public void validateCanKickChatMember(long chatId, long adminId, long otherUserId) {
        validateActiveUsers(adminId, otherUserId);
        validateActiveChat(chatId);
        validateActiveChatMemberIsAdmin(chatId, adminId);
        validateActiveChatMember(chatId, otherUserId);
    }

    public void validateCanUpdateMessage(long chatId, long userId, long messageId) {
        validateActiveUser(userId);
        Optional<Boolean> isAdmin = dataOrchestrator.isActiveAdminInActiveChat(chatId, userId);
        if (isAdmin.isEmpty()) {
            throw new ValidationException("Member does not exist or is deleted -> " + userId);
        }

        if (isAdmin.get()) {
            validateActiveMessageInChat(chatId, messageId);
        } else {
            validateActiveMessageInChatAndIsSender(chatId, userId, messageId);
        }
    }
    public void validateCanDeleteMessage(long chatId, long userId, long messageId) {
        validateActiveUser(userId);
        Optional<Boolean> isAdmin = dataOrchestrator.isActiveAdminInActiveChat(chatId, userId);
        if (isAdmin.isEmpty()) {
            throw new ValidationException("Member does not exist or is deleted -> " + userId);
        }

        if (isAdmin.get()) {
            validateActiveMessageInChat(chatId, messageId);
        } else {
            validateActiveMessageInChatAndIsSender(chatId, userId, messageId);
        }
    }


    // ========== OTHER METHODS ==========

    public void validateActiveChatMemberInActiveChat(long chatId, long userId) {
        validateActiveUser(userId);
        validateActiveChat(chatId);
        validateActiveChatMember(chatId, userId);
    }
    public ChatSecurityDTO validateActiveUserInActiveChatAndGetChat(long chatId, long userId) {
        validateActiveUser(userId);
        ChatSecurityDTO chat = validateActiveChatAndGet(chatId);
        validateActiveChatMember(chatId, userId);
        return chat;
    }
}
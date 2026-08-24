package com.sunrise.orchestrator;

import com.sunrise.helpclass.exception.MyErrorCode;
import com.sunrise.helpclass.exception.MyException;
import com.sunrise.orchestrator.result.Dto.*;
import com.sunrise.orchestrator.service.ChatMemberOrchestrator;
import com.sunrise.orchestrator.service.ChatOrchestrator;
import com.sunrise.orchestrator.service.MessageOrchestrator;
import com.sunrise.orchestrator.service.UserOrchestrator;
import com.sunrise.orchestrator.type.ChatType;

import lombok.RequiredArgsConstructor;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataValidator {

    private final UserOrchestrator userOrchestrator;
    private final ChatOrchestrator chatOrchestrator;
    private final ChatMemberOrchestrator chatMemberOrchestrator;
    private final MessageOrchestrator messageOrchestrator;


    // ========== BASE METHODS ==========

    public void validateActiveUser(long userId) {
        if (!userOrchestrator.isActiveUser(userId)) {
            throw new MyException(MyErrorCode.USER_NOT_ACTIVE, "User is not active -> " + userId);
        }
    }

    public void validateActiveUsers(long userId, long otherUserId) {
        if (userId == otherUserId) {
            throw new MyException(MyErrorCode.VALIDATION_ERROR, "User equals to other user -> " + userId);
        }
        validateActiveUser(userId);
        validateActiveUser(otherUserId);
    }

    private void validateActiveUsers(long userId, @NonNull Set<Long> userIds) {
        if (userIds.contains(userId)) {
            throw new MyException(MyErrorCode.VALIDATION_ERROR, "User in users list -> " + userId);
        }
        validateActiveUser(userId);
        userIds.forEach(this::validateActiveUser);
    }

    private void validateActiveChat(long chatId) {
        if (!chatOrchestrator.isActive(chatId)) {
            throw new MyException(MyErrorCode.CHAT_NOT_FOUND_OR_DELETED, "Chat does not exist or is deleted -> " + chatId);
        }
    }

    private void validateActiveChatMember(long chatId, long userId) {
        if (!chatMemberOrchestrator.hasActive(chatId, userId)) {
            throw new MyException(MyErrorCode.MEMBER_NOT_FOUND, "User " + userId + " is not a member of the chat " + chatId);
        }
    }

    private void validateNotActiveChatMember(long chatId, long userId) {
        if (chatMemberOrchestrator.hasActive(chatId, userId)) {
            throw new MyException(MyErrorCode.MEMBER_ALREADY_EXISTS, "User " + userId + " is already a member of the chat " + chatId);
        }
    }

    private void validateActiveGroupChat(long chatId) {
        Optional<Boolean> isGroup = chatOrchestrator.isActiveGroupChat(chatId);
        if (isGroup.isEmpty()) {
            throw new MyException(MyErrorCode.CHAT_NOT_FOUND_OR_DELETED, "Chat does not exist or is deleted -> " + chatId);
        }
        if (!isGroup.get()) {
            throw new MyException(MyErrorCode.CHAT_NOT_GROUP, "Chat is a personal chat -> " + chatId);
        }
    }

    private ChatSecurity validateActiveChatAndGet(long chatId) {
        Optional<ChatSecurity> chatOpt = chatOrchestrator.getActive(chatId);
        if (chatOpt.isEmpty()) {
            throw new MyException(MyErrorCode.CHAT_NOT_FOUND_OR_DELETED, "Chat does not exist or is deleted -> " + chatId);
        }
        return chatOpt.get();
    }

    private void validateActiveChatMemberIsAdmin(long chatId, long userId) {
        Optional<Boolean> isAdmin = chatMemberOrchestrator.isActiveAdmin(chatId, userId);
        if (isAdmin.isEmpty()) {
            throw new MyException(MyErrorCode.MEMBER_NOT_FOUND_OR_DELETED, "User " + userId + " is not a member or is deleted from the chat " + chatId);
        }
        if (!isAdmin.get()) {
            throw new MyException(MyErrorCode.MEMBER_NOT_ADMIN, "User is not group admin -> " + userId);
        }
    }

    public void validateActiveMessageInChat(long chatId, long messageId) {
        if (!messageOrchestrator.isActiveInChat(chatId, messageId)) {
            throw new MyException(MyErrorCode.MESSAGE_NOT_FOUND_OR_DELETED, "Message does not exist or is deleted -> " + messageId);
        }
    }

    private void validateActiveMessageInChatAndIsSender(long chatId, long userId, long messageId) {
        if (!messageOrchestrator.isActiveInChatAndBySender(chatId, userId, messageId)) {
            throw new MyException(MyErrorCode.MESSAGE_NOT_SENDER, "Message does not exist, is deleted, or user is not sender -> " + messageId);
        }
    }


    // ========== COMPLEX METHODS ==========

    public void validateCanCreateGroupChat(long userId, Set<Long> userIds) {
        if (!ChatType.GROUP.isMembersInBound(userIds.size() + 1)) {
            throw new MyException(MyErrorCode.CHAT_MEMBERS_LIMIT, "Members count out of bounds for GROUP chat");
        }
        validateActiveUsers(userId, userIds);
    }

    public void validateCanUpdateChatInfo(long chatId, long userId) {
        validateActiveUser(userId);
        ChatSecurity chat = validateActiveChatAndGet(chatId);
        if (chat.chatType().isPersonal()) {
            throw new MyException(MyErrorCode.CHAT_INFO_NOT_CHANGEABLE, "Chat info is not changeable for personal chat");
        }
        validateActiveChatMemberIsAdmin(chatId, userId);
    }

    public void validateCanDeleteChat(long chatId, long userId) {
        validateActiveChatMemberInActiveChat(chatId, userId);
        validateActiveChatMemberIsAdmin(chatId, userId);
    }

    public void validateCanAddChatMembers(long chatId, long userId, @NonNull Set<Long> newUserIds) {
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
        Optional<Boolean> isAdmin = chatMemberOrchestrator.isActiveAdmin(chatId, userId);
        if (isAdmin.isEmpty()) {
            throw new MyException(MyErrorCode.MEMBER_NOT_FOUND_OR_DELETED, "User " + userId + " is not a member or is deleted from the chat " + chatId);
        }
        if (isAdmin.get()) {
            validateActiveMessageInChat(chatId, messageId);
        } else {
            validateActiveMessageInChatAndIsSender(chatId, userId, messageId);
        }
    }

    public void validateCanDeleteMessage(long chatId, long userId, long messageId) {
        validateActiveUser(userId);
        Optional<Boolean> isAdmin = chatMemberOrchestrator.isActiveAdmin(chatId, userId);
        if (isAdmin.isEmpty()) {
            throw new MyException(MyErrorCode.MEMBER_NOT_FOUND_OR_DELETED, "User " + userId + " is not a member or is deleted from the chat " + chatId);
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

    public ChatSecurity validateActiveUserInActiveChatAndGetChat(long chatId, long userId) {
        validateActiveUser(userId);
        ChatSecurity chat = validateActiveChatAndGet(chatId);
        validateActiveChatMember(chatId, userId);
        return chat;
    }
}
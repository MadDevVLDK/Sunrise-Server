package com.sunrise.web.websocket.controller;

import com.sunrise.core.service.ChatService;
import com.sunrise.core.service.MessageService;
import com.sunrise.helpclass.exception.MyException;
import com.sunrise.web.api.annotation.ValidId;
import com.sunrise.web.payload.ApiRequest;
import com.sunrise.web.websocket.annotation.WsCurrentUserId;
import com.sunrise.web.websocket.service.UserGlobalStatusKeeper;
import com.sunrise.web.websocket.service.WebSocketNotifier;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Slf4j
@RequiredArgsConstructor
@Controller
public class WebSocketController {

    private final MessageService messageService;
    private final WebSocketNotifier wsNotify;
    private final UserGlobalStatusKeeper userGlobalStatusKeeper;
    private final ChatService chatService;


    // =========================== MESSAGE ===========================

    @MessageMapping("/chats/{chatId}/messages/send")
    public void sendMessage(@DestinationVariable("chatId") @ValidId long chatId,
                            @Payload ApiRequest.Message request,
                            @WsCurrentUserId long userId,
                            @Header("simpSessionId") String sessionId,
                            @Header("simpDestination") String errorUrl) {
        try {
            messageService.makePublicMessage(request.tempId(), chatId, userId, request.text());
        } catch (MyException e) {
            log.warn("[🔌] ☝️ Failed to send message in chat {}: code={}, msg={}", chatId, e.getCode(), e.getMessage());
            wsNotify.notifyError(userId, sessionId, e, errorUrl);
        }
    }

    @MessageMapping("/chats/{chatId}/messages/{messageId}/edit")
    public void editMessage(@DestinationVariable("chatId") @ValidId long chatId,
                            @DestinationVariable("messageId") @ValidId long messageId,
                            @Payload ApiRequest.UpdateMessage request,
                            @WsCurrentUserId long userId,
                            @Header("simpSessionId") String sessionId,
                            @Header("simpDestination") String errorUrl) {
        try {
            messageService.updateMessage(chatId, userId, messageId, request.text());
        } catch (MyException e) {
            log.warn("[🔌] ☝️ Failed to edit message {} in chat {}: code={}, msg={}", messageId, chatId, e.getCode(), e.getMessage());
            wsNotify.notifyError(userId, sessionId, e, errorUrl);
        }
    }

    @MessageMapping("/chats/{chatId}/messages/{messageId}/delete")
    public void deleteMessage(@DestinationVariable("chatId") @ValidId long chatId,
                              @DestinationVariable("messageId") @ValidId long messageId,
                              @WsCurrentUserId long userId,
                              @Header("simpSessionId") String sessionId,
                              @Header("simpDestination") String errorUrl) {
        try {
            messageService.deleteMessage(chatId, userId, messageId);
        } catch (MyException e) {
            log.warn("[🔌] ☝️ Failed to delete message {} in chat {}: code={}, msg={}", messageId, chatId, e.getCode(), e.getMessage());
            wsNotify.notifyError(userId, sessionId, e, errorUrl);
        }
    }

    @MessageMapping("/chats/{chatId}/messages/{messageId}/up-to-read")
    public void markMessagesAsReadUpTo(@DestinationVariable("chatId") @ValidId long chatId,
                                       @DestinationVariable("messageId") @ValidId long messageId,
                                       @WsCurrentUserId long userId,
                                       @Header("simpSessionId") String sessionId,
                                       @Header("simpDestination") String errorUrl) {
        try {
            messageService.markMessagesUpToRead(chatId, userId, messageId);
        } catch (MyException e) {
            log.warn("[🔌] ☝️ Failed to mark messages read in chat {}: code={}, msg={}", chatId, e.getCode(), e.getMessage());
            wsNotify.notifyError(userId, sessionId, e, errorUrl);
        }
    }


    // =========================== ACTIONS / PRESENCE / OTHER ===========================

    @MessageMapping("/users/{userIdToSub}/status/get")
    public void getUserGlobalStatus(@DestinationVariable("userIdToSub") @ValidId long userIdToSub,
                                    @WsCurrentUserId long userId,
                                    @Header("simpSessionId") String sessionId,
                                    @Header("simpDestination") String errorUrl) {
        userGlobalStatusKeeper.getUserStatus(userIdToSub)
            .ifPresent(status -> wsNotify.notifyUserStatusToSubscriber(userId, sessionId, userIdToSub, status));
    }

    @MessageMapping("/users/{userIdToSub}/status/update/{status}")
    public void updateUserGlobalStatus(@DestinationVariable("userIdToSub") @ValidId long userIdToSub,
                                       @DestinationVariable("status") String status,
                                       @WsCurrentUserId long userId,
                                       @Header("simpSessionId") String sessionId,
                                       @Header("simpDestination") String errorUrl) {
        if (userIdToSub != userId) {
            wsNotify.notifyError(userId, sessionId, "You cannot change other user status", errorUrl);
            return;
        }

        if (userGlobalStatusKeeper.updateUserStatus(userId, status)) {
            wsNotify.notifyUserStatus(userId, status);
        }

        if ("offline".equalsIgnoreCase(status)) {
            userGlobalStatusKeeper.removeUserActions(userId);
        }
    }

    @MessageMapping("/chats/{chatId}/actions/get")
    public void getChatActions(@DestinationVariable("chatId") @ValidId long chatId,
                               @WsCurrentUserId long userId,
                               @Header("simpSessionId") String sessionId,
                               @Header("simpDestination") String errorUrl) {
        try {
            boolean isEnabled = chatService.isActionsEnabledForChat(chatId, userId);
            if (!isEnabled) {
                wsNotify.notifyError(userId, sessionId, "Actions are disabled for this chat", errorUrl);
                return;
            }

            Map<Long, String> actions = userGlobalStatusKeeper.getChatActions(chatId);
            for (Map.Entry<Long, String> entry : actions.entrySet()) {
                wsNotify.notifyUserChatActionToSubscriber(userId, sessionId, chatId, entry.getKey(), entry.getValue());
            }
        } catch (MyException e) {
            log.warn("[🔌] ☝️ Failed to get chat actions for chat {}: code={}, msg={}", chatId, e.getCode(), e.getMessage());
            wsNotify.notifyError(userId, sessionId, e, errorUrl);
        }
    }

    @MessageMapping("/chats/{chatId}/actions/update/{action}")
    public void updateUserChatAction(@DestinationVariable("chatId") @ValidId long chatId,
                                     @DestinationVariable("action") String action,
                                     @WsCurrentUserId long userId,
                                     @Header("simpSessionId") String sessionId,
                                     @Header("simpDestination") String errorUrl) {
        try {
            boolean isEnabled = chatService.isActionsEnabledForChat(chatId, userId);
            if (!isEnabled) {
                wsNotify.notifyError(userId, sessionId, "Actions are disabled for this chat", errorUrl);
                return;
            }

            if (userGlobalStatusKeeper.updateUserAction(chatId, userId, action)) {
                wsNotify.notifyUserChatAction(chatId, userId, action);
            }
        } catch (MyException e) {
            log.warn("[🔌] ☝️ Failed to update chat action for chat {}: code={}, msg={}", chatId, e.getCode(), e.getMessage());
            wsNotify.notifyError(userId, sessionId, e, errorUrl);
        }
    }

    @MessageMapping("/ping")
    public void ping(@WsCurrentUserId long userId, @Header("simpSessionId") String sessionId) {
        wsNotify.notifyPong(userId, sessionId);
    }
}
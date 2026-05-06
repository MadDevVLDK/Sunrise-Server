package com.sunrise.web.websocket.controller;

import com.sunrise.web.api.annotation.ValidId;
import com.sunrise.web.payload.ApiRequest;
import com.sunrise.web.websocket.annotation.WsCurrentUserId;
import com.sunrise.web.websocket.service.UserGlobalStatusKeeper;
import com.sunrise.web.websocket.service.WebSocketNotifier;
import com.sunrise.core.result.ResultNoArgs;
import com.sunrise.core.result.ResultOneArg;
import com.sunrise.core.service.ChatService;
import com.sunrise.core.service.MessageService;

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
    public void sendMessage(@DestinationVariable("chatId") @ValidId long chatId, @Payload ApiRequest.Message request,
                            @WsCurrentUserId long userId, @Header("simpSessionId") String sessionId, 
                            @Header("simpDestination") String errorUrl) {

        ResultOneArg<Long> result = messageService.makePublicMessage(request.tempId(), chatId, userId, request.text());
        if (!result.isSuccess()) {
            wsNotify.notifyError(userId, sessionId, result.getError(), errorUrl);
        }
    }

    @MessageMapping("/chats/{chatId}/messages/{messageId}/edit")
    public void editMessage(@DestinationVariable("chatId") long chatId, @DestinationVariable("messageId") long messageId, @Payload ApiRequest.UpdateMessage request,
                            @WsCurrentUserId long userId, @Header("simpSessionId") String sessionId, @Header("simpDestination") String errorUrl) {

        ResultNoArgs result = messageService.updateMessage(chatId, userId, messageId, request.text());
        if (!result.isSuccess()) {
            wsNotify.notifyError(userId, sessionId, result.getError(), errorUrl);
        }
    }

    @MessageMapping("/chats/{chatId}/messages/{messageId}/delete")
    public void deleteMessage(@DestinationVariable("chatId") @ValidId long chatId, 
                              @DestinationVariable("messageId") @ValidId long messageId,
                              @WsCurrentUserId long userId, @Header("simpSessionId") String sessionId, 
                              @Header("simpDestination") String errorUrl) {

        ResultNoArgs result = messageService.deleteMessage(chatId, userId, messageId);
        if (!result.isSuccess()) {
            wsNotify.notifyError(userId, sessionId, result.getError(), errorUrl);
        }
    }

    @MessageMapping("/chats/{chatId}/messages/{messageId}/up-to-read")
    public void markMessagesAsReadUpTo(@DestinationVariable("chatId") @ValidId long chatId, 
                                       @DestinationVariable("messageId") @ValidId long messageId,
                                       @WsCurrentUserId long userId, @Header("simpSessionId") String sessionId, 
                                       @Header("simpDestination") String errorUrl) {

        ResultNoArgs result = messageService.markMessagesUpToRead(chatId, userId, messageId);
        if (!result.isSuccess()) {
            wsNotify.notifyError(userId, sessionId, result.getError(), errorUrl);
        }
    }


    // =========================== ACTIONS/PRESENCE/OTHER ===========================

    @MessageMapping("/users/{userIdToSub}/status/get")
    public void getUserGlobalStatus(@DestinationVariable("userIdToSub") @ValidId long userIdToSub, @WsCurrentUserId long userId,
                                    @Header("simpSessionId") String sessionId, @Header("simpDestination") String errorUrl) {
        
        userGlobalStatusKeeper.getUserStatus(userIdToSub)
            .ifPresent(status -> wsNotify.notifyUserStatusChangeToSubscriber(userId, sessionId, userIdToSub, status));
    }
    
    @MessageMapping("/users/{userIdToSub}/status/update/{status}")
    public void updateUserGlobalStatus(@DestinationVariable("userIdToUpdate") @ValidId long userIdToUpdate, 
                                       @DestinationVariable("status") String status, 
                                       @WsCurrentUserId long userId, @Header("simpSessionId") String sessionId, 
                                       @Header("simpDestination") String errorUrl) {
        
        if (userIdToUpdate != userId){
            wsNotify.notifyError(userId, sessionId, "you cannot change other user status", errorUrl);
            return;
        }
        
        if (userGlobalStatusKeeper.updateUserStatus(userId, status)){
            wsNotify.notifyUserStatusChange(userId, status);
        }

        if ("offline".equalsIgnoreCase(status)) {
            userGlobalStatusKeeper.removeUserActions(userId);
        }
    }


    @MessageMapping("/chats/{chatId}/actions/get")
    public void getChatActions(@DestinationVariable("chatId") @ValidId long chatId, @WsCurrentUserId long userId,
                               @Header("simpSessionId") String sessionId, @Header("simpDestination") String errorUrl) {
        
        ResultOneArg<Boolean> result = chatService.isActionsEnabledForChat(chatId, userId);
        if (!result.isSuccess() || !Boolean.TRUE.equals(result.getResult())) {
            wsNotify.notifyError(userId, sessionId, "Cannot get chat actions", errorUrl);
            return;
        }

        Map<Long, String> actions = userGlobalStatusKeeper.getChatActions(chatId);
        for (Map.Entry<Long, String> entry : actions.entrySet()) {
            wsNotify.notifyUserActionToSubscriber(userId, sessionId, chatId, entry.getKey(), entry.getValue());
        }
    }

    @MessageMapping("/chats/{chatId}/actions/update/{action}")
    public void updateUserChatAction(@DestinationVariable("chatId") @ValidId long chatId, @DestinationVariable("action") String action,
                                     @WsCurrentUserId @ValidId long userId, @Header("simpSessionId") String sessionId, 
                                     @Header("simpDestination") String errorUrl) {

        ResultOneArg<Boolean> result = chatService.isActionsEnabledForChat(chatId, userId);
        if (!result.isSuccess() || !Boolean.TRUE.equals(result.getResult())){
            wsNotify.notifyError(userId, sessionId, "Cannot update chat actions", errorUrl);
            return;
        }

        if (userGlobalStatusKeeper.updateUserAction(chatId, userId, action)){
            wsNotify.notifyUserAction(chatId, userId, action);
        }
    }


    @MessageMapping("/ping")
    public void ping(@WsCurrentUserId long userId, @Header("simpSessionId") String sessionId) {
        wsNotify.notifyPong(userId, sessionId);
    }
}
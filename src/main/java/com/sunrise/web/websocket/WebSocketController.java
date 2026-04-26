package com.sunrise.web.websocket;

import com.sunrise.web.api.annotation.ValidId;
import com.sunrise.web.websocket.annotation.WsCurrentUserId;
import com.sunrise.web.websocket.request.WsRequests;
import com.sunrise.notifier.WebSocketNotifier;
import com.sunrise.service.ChatService;
import com.sunrise.service.MessageService;
import com.sunrise.service.result.ResultNoArgs;
import com.sunrise.service.result.ResultOneArg;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Set;

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
    public void sendMessage(@DestinationVariable long chatId, @Payload WsRequests.MessageNewRequest request,
                            @WsCurrentUserId long userId, Principal principal, @Header("simpDestination") String errorUrl) {

        if (principal == null || principal.getName() == null) {
            log.error("[⚠️] Invalid principal in sendMessage");
            return;
        }

        ResultOneArg<Long> result = messageService.makePublicMessage(request.tempId(), chatId, userId, request.text());
        if (!result.isSuccess()) {
            wsNotify.notifyError(principal.getName(), result.getError(), errorUrl);
        }
    }
    @MessageMapping("/chats/{chatId}/messages/send-private")
    public void sendPrivateMessage(@DestinationVariable long chatId, @Payload WsRequests.MessagePrivateNewRequest request,
                            @WsCurrentUserId long userId, Principal principal, @Header("simpDestination") String errorUrl) {

        if (principal == null || principal.getName() == null) {
            log.error("[⚠️] Invalid principal in sendPrivateMessage");
            return;
        }

        ResultOneArg<Long> result = messageService.makePrivateMessage(request.tempId(), chatId, userId, request.receiverId(), request.text());
        if (!result.isSuccess()) {
            wsNotify.notifyError(principal.getName(), result.getError(), errorUrl);
        }
    }
    @MessageMapping("/chats/{chatId}/messages/edit")
    public void editMessage(@DestinationVariable long chatId, @Payload WsRequests.MessageInfoUpdateRequest request,
                            @WsCurrentUserId long userId, Principal principal, @Header("simpDestination") String errorUrl) {

        if (principal == null || principal.getName() == null) {
            log.error("[⚠️] Invalid principal in editMessage");
            return;
        }

        ResultNoArgs result = messageService.updateMessage(chatId, userId, request.messageId(), request.newText());
        if (!result.isSuccess()) {
            wsNotify.notifyError(principal.getName(), result.getError(), errorUrl);
        }
    }
    @MessageMapping("/chats/{chatId}/messages/delete")
    public void deleteMessage(@DestinationVariable long chatId, @Payload WsRequests.MessageDeleteRequest request,
                              @WsCurrentUserId long userId, Principal principal, @Header("simpDestination") String errorUrl) {

        if (principal == null || principal.getName() == null) {
            log.error("[⚠️] Invalid principal in deleteMessage");
            return;
        }

        ResultNoArgs result = messageService.deleteMessage(chatId, userId, request.messageId());
        if (!result.isSuccess()) {
            wsNotify.notifyError(principal.getName(), result.getError(), errorUrl);
        }
    }

    @MessageMapping("/chats/{chatId}/messages/read")
    public void markMessagesAsReadUpTo(@DestinationVariable long chatId, @Payload WsRequests.MarkAsReadRequest request,
                                       @WsCurrentUserId long userId, Principal principal, @Header("simpDestination") String errorUrl) {

        if (principal == null || principal.getName() == null) {
            log.error("[⚠️] Invalid principal in markMessagesAsReadUpTo");
            return;
        }

        ResultNoArgs result = messageService.markMessagesUpToRead(chatId, userId, request.upToMessageId());
        if (!result.isSuccess()) {
            wsNotify.notifyError(principal.getName(), result.getError(), errorUrl);
        }
    }


    // =========================== ACTIONS/PRESENCE/OTHER ===========================

    @MessageMapping("subscribe/user-status/{userId}")
    public void subscribeUserGlobalStatus(@DestinationVariable @ValidId long userId, Principal principal) {
        if (principal == null || principal.getName() == null) {
            log.error("[⚠️] Invalid principal in subscribeUserGlobalStatus");
            return;
        }
        userGlobalStatusKeeper.subscribeUserGlobalStatus(userId, principal.getName());
    }
    @MessageMapping("/user-status/{status}")
    public void updateUserGlobalStatus(@WsCurrentUserId long userId, @DestinationVariable String status) {

        Set<String> sessionsToNotify = userGlobalStatusKeeper.updateUserGlobalStatus(userId, status);
        if (!sessionsToNotify.isEmpty()){
            wsNotify.notifyUserStatusChange(userId, status, sessionsToNotify);
        }
    }
    @MessageMapping("unsubscribe/user-status/{userId}")
    public void unsubscribeUserGlobalStatus(@DestinationVariable @ValidId long userId, Principal principal) {
        if (principal == null || principal.getName() == null) {
            log.error("[⚠️] Invalid principal in unsubscribeUserGlobalStatus");
            return;
        }
        userGlobalStatusKeeper.unsubscribeUserGlobalStatus(userId, principal.getName());
    }

    @MessageMapping("/chats/{chatId}/actions/{action}")
    public void updateUserChatAction(@DestinationVariable long chatId, @DestinationVariable String action,
                                     @WsCurrentUserId long userId, Principal principal, @Header("simpDestination") String errorUrl) {

        if (principal == null || principal.getName() == null) {
            log.error("[⚠️] Invalid principal in updateUserChatAction");
            return;
        }

        ResultOneArg<Boolean> result = chatService.isActionsEnabledForChat(chatId, userId);
        if (!result.isSuccess()){
            wsNotify.notifyError(principal.getName(), result.getError(), errorUrl);
            return;
        }

        if (!Boolean.TRUE.equals(result.getResult())) {
            wsNotify.notifyError(principal.getName(), "Actions is not enabled for this chatType", errorUrl);
            return;
        }

        if (userGlobalStatusKeeper.updateUserAction(chatId, userId, action)){
            wsNotify.notifyUserAction(chatId, userId, action);
        }
    }

    @MessageMapping("/ping")
    public void ping(Principal principal) {
        if (principal == null || principal.getName() == null) {
            log.error("[⚠️] Invalid principal in ping");
            return;
        }
        wsNotify.notifyPong(principal.getName());
    }
}
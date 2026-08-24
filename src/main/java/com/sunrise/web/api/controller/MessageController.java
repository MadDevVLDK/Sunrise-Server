package com.sunrise.web.api.controller;

import com.sunrise.core.service.MessageService;
import com.sunrise.orchestrator.result.Dto.*;
import com.sunrise.web.api.annotation.CurrentUserId;
import com.sunrise.web.api.annotation.ValidId;
import com.sunrise.web.payload.ApiRequest;
import com.sunrise.web.payload.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/chats/{chatId}/messages")
public class MessageController {

    private final MessageService messageService;

    @PostMapping
    public ResponseEntity<?> sendPublicMessage(@PathVariable("chatId") @ValidId long chatId,
                                               @RequestBody @Valid ApiRequest.Message request,
                                               @CurrentUserId long userId) {
        Long messageId = messageService.makePublicMessage(request.tempId(), chatId, userId, request.text());
        return ApiResponse.success(messageId);
    }

    @PutMapping("/{messageId}")
    public ResponseEntity<?> updateMessage(@PathVariable("chatId") @ValidId long chatId,
                                           @PathVariable("messageId") @ValidId long messageId,
                                           @RequestBody @Valid ApiRequest.UpdateMessage request,
                                           @CurrentUserId long userId) {
        messageService.updateMessage(chatId, userId, messageId, request.text().trim());
        return ApiResponse.success();
    }

    @PutMapping("/{messageId}/mark-up-to-read")
    public ResponseEntity<?> markMessagesUpToRead(@PathVariable("chatId") @ValidId long chatId,
                                                  @PathVariable("messageId") @ValidId long messageId,
                                                  @CurrentUserId long userId) {
        messageService.markMessagesUpToRead(chatId, userId, messageId);
        return ApiResponse.success();
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<?> deleteMessage(@PathVariable("chatId") @ValidId long chatId,
                                           @PathVariable("messageId") @ValidId long messageId,
                                           @CurrentUserId long userId) {
        messageService.deleteMessage(chatId, userId, messageId);
        return ApiResponse.success();
    }

    @GetMapping("/{messageId}/reads")
    public ResponseEntity<?> getMessageReads(@PathVariable("chatId") @ValidId long chatId,
                                             @PathVariable("messageId") @ValidId long messageId,
                                             @CurrentUserId long userId) {
        List<MessageReadStatus> result = messageService.getMessageReads(chatId, userId, messageId);
        return ApiResponse.success(result);
    }

    @GetMapping("/{messageId}")
    public ResponseEntity<?> getMessage(@PathVariable("chatId") @ValidId long chatId,
                                        @PathVariable("messageId") @ValidId long messageId,
                                        @CurrentUserId long userId) {
        Message result = messageService.getMessage(chatId, userId, messageId);
        return ApiResponse.success(result);
    }

    @GetMapping("/batch")
    public ResponseEntity<?> getMessageBatch(@PathVariable("chatId") @ValidId long chatId,
                                             @Valid ApiRequest.Batch request,
                                             @CurrentUserId long userId) {
        List<Message> result = messageService.getMessageBatch(chatId, userId, request.ids());
        return ApiResponse.success(result);
    }

    @GetMapping
    public ResponseEntity<?> getMessagesPage(@PathVariable("chatId") @ValidId long chatId,
                                             @Valid ApiRequest.MessagePagination request,
                                             @CurrentUserId long userId) {
        MessagesPage result = messageService.getMessagePagination(
            chatId, userId, request.cursor(), request.getLimit(), request.direction()
        );
        return ApiResponse.success(result);
    }
}
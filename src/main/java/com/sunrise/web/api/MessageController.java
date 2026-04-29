package com.sunrise.web.api;

import com.sunrise.web.api.annotation.CurrentUserId;
import com.sunrise.web.api.annotation.ValidId;
import com.sunrise.web.payload.ApiRequest;
import com.sunrise.web.payload.ApiResponse;
import com.sunrise.core.result.*;
import com.sunrise.core.service.MessageService;
import com.sunrise.orchestrator.result.MessageReadStatusDTO;
import com.sunrise.orchestrator.result.MessagesPageDTO;
import com.sunrise.orchestrator.result.UserMessageDTO;

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
                                               @RequestBody @Valid ApiRequest.PublicMessage request, @CurrentUserId long userId) {

        ResultOneArg<Long> result = messageService.makePublicMessage(request.tempId(), chatId, userId, request.text());

        return result.isSuccess() ?
                ApiResponse.success(result.getResult()) :
                ApiResponse.error(result.getError());
    }

    @PostMapping("/private")
    public ResponseEntity<?> sendPrivateMessage(@PathVariable("chatId") @ValidId long chatId,
                                                @RequestBody @Valid ApiRequest.PrivateMessage request, @CurrentUserId long userId) {

        ResultOneArg<Long> result = messageService.makePrivateMessage(
            request.tempId(), chatId, userId, request.receiverId(), request.text()
        );

        return result.isSuccess() ?
                ApiResponse.success(result.getResult()) :
                ApiResponse.error(result.getError());
    }


    @PutMapping("/{messageId}")
    public ResponseEntity<?> updateMessage(@PathVariable("chatId") @ValidId long chatId, @PathVariable("messageId") @ValidId long messageId,
                                           @RequestBody @Valid ApiRequest.UpdateMessage request, @CurrentUserId long userId) {

        ResultNoArgs result = messageService.updateMessage(
            chatId, userId, messageId, request.text().trim()
        );

        return result.isSuccess() ?
                ApiResponse.success() :
                ApiResponse.error(result.getError());
    }

    @PutMapping("/{messageId}/mark-up-to-read")
    public ResponseEntity<?> markMessagesUpToRead(@PathVariable("chatId") @ValidId long chatId, @PathVariable("messageId") @ValidId long messageId, 
                                                  @CurrentUserId long userId) {

        ResultNoArgs result = messageService.markMessagesUpToRead(chatId, userId, messageId);

        return result.isSuccess() ?
                ApiResponse.success() :
                ApiResponse.error(result.getError());
    }


    @DeleteMapping("/{messageId}")
    public ResponseEntity<?> deleteMessage(@PathVariable("chatId") @ValidId long chatId, @PathVariable("messageId") @ValidId long messageId, 
                                           @CurrentUserId long userId) {

        ResultNoArgs result = messageService.deleteMessage(chatId, userId, messageId);

        return result.isSuccess() ?
                ApiResponse.success() :
                ApiResponse.error(result.getError());
    }


    @GetMapping("/{messageId}/reads")
    public ResponseEntity<?> getMessageReads(@PathVariable("chatId") @ValidId long chatId, @PathVariable("messageId") @ValidId long messageId, 
                                             @CurrentUserId long userId) {

        ResultOneArg<List<MessageReadStatusDTO>> result = messageService.getMessageReads(chatId, userId, messageId);

        return result.isSuccess() ?
                ApiResponse.success(result.getResult()) :
                ApiResponse.error(result.getError());
    }

    @GetMapping("/{messageId}")
    public ResponseEntity<?> getMessage(@PathVariable("chatId") @ValidId long chatId, @PathVariable("messageId") @ValidId long messageId, 
                                        @CurrentUserId long userId) {

        ResultOneArg<UserMessageDTO> result = messageService.getMessage(chatId, userId, messageId);

        return result.isSuccess() ?
                ApiResponse.success(result.getResult()) :
                ApiResponse.error(result.getError());
    }

    @GetMapping("/batch")
    public ResponseEntity<?> getMessage(@PathVariable("chatId") @ValidId long chatId, @Valid ApiRequest.Batch request, 
                                        @CurrentUserId long userId) {

        ResultOneArg<List<UserMessageDTO>> result = messageService.getMessageBatch(chatId, userId, request.ids());

        return result.isSuccess() ?
                ApiResponse.success(result.getResult()) :
                ApiResponse.error(result.getError());
    }

    @GetMapping
    public ResponseEntity<?> getMessagesPage(@PathVariable("chatId") @ValidId long chatId, 
                                             @Valid ApiRequest.MessagePagination request, @CurrentUserId long userId) {

        ResultOneArg<MessagesPageDTO> result = messageService.getMessagePagination(chatId, userId, request.cursor(), request.getLimit(), request.direction());

        return result.isSuccess() ?
                ApiResponse.success(result.getResult()) :
                ApiResponse.error(result.getError());
    }
}
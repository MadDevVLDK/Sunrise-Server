package com.sunrise.controller;

import com.sunrise.config.annotation.CurrentUserId;
import com.sunrise.config.annotation.ValidId;

import com.sunrise.controller.request.PaginationRequest;
import com.sunrise.controller.request.PrivateMessageRequest;
import com.sunrise.controller.request.PublicMessageRequest;
import com.sunrise.controller.request.UpdateMessageRequest;
import com.sunrise.core.service.result.*;

import com.sunrise.core.dataservice.type.Direction;
import com.sunrise.core.service.MessageService;

import com.sunrise.entity.dto.UserMessageDTO;
import com.sunrise.entity.dto.MessageReadStatusDTO;
import com.sunrise.entity.pagination.MessagesPageDTO;
import jakarta.validation.Valid;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/chats/{chatId}/messages")
public class MessageController {

    private final MessageService messageService;

    @PostMapping
    public ResponseEntity<?> sendPublicMessage(@PathVariable @ValidId long chatId,
                                               @RequestBody @Valid PublicMessageRequest request, @CurrentUserId long userId) {

        ResultOneArg<Long> result = messageService.makePublicMessage(request.getTempId(), chatId, userId, request.getText());

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getResult());
        } else {
            return ResponseEntity.badRequest().body(result.getError());
        }
    }

    @PostMapping("/private")
    public ResponseEntity<?> sendPrivateMessage(@PathVariable @ValidId long chatId,
                                                @RequestBody @Valid PrivateMessageRequest request, @CurrentUserId long userId) {

        ResultOneArg<Long> result = messageService.makePrivateMessage(request.getTempId(), chatId, userId, request.getUserToSendId(), request.getText());

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getResult());
        } else {
            return ResponseEntity.badRequest().body(result.getError());
        }
    }


    @PutMapping("/{messageId}")
    public ResponseEntity<?> updateMessage(@PathVariable @ValidId long chatId, @PathVariable @ValidId long messageId,
                                           @RequestBody @Valid UpdateMessageRequest request, @CurrentUserId long userId) {

        ResultNoArgs result = messageService.updateMessage(chatId, userId, messageId, request.getText().trim());

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getOperationText());
        } else {
            return ResponseEntity.badRequest().body(result.getError());
        }
    }

    @PutMapping("/{messageId}/mark-up-to-read")
    public ResponseEntity<?> markMessagesUpToRead(@PathVariable @ValidId long chatId, @PathVariable @ValidId long messageId, @CurrentUserId long userId) {

        ResultNoArgs result = messageService.markMessagesUpToRead(chatId, userId, messageId);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getOperationText());
        } else {
            return ResponseEntity.badRequest().body(result.getError());
        }
    }


    @DeleteMapping("/{messageId}")
    public ResponseEntity<?> deleteMessage(@PathVariable @ValidId long chatId, @PathVariable @ValidId long messageId, @CurrentUserId long userId) {

        ResultNoArgs result = messageService.deleteMessage(chatId, userId, messageId);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getOperationText());
        } else {
            return ResponseEntity.badRequest().body(result.getError());
        }
    }


    @GetMapping("/{messageId}/reads")
    public ResponseEntity<?> getMessageReads(@PathVariable @ValidId long chatId, @PathVariable @ValidId long messageId, @CurrentUserId long userId) {

        ResultOneArg<Map<Long, MessageReadStatusDTO>> result = messageService.getMessageReads(chatId, userId, messageId);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getResult());
        } else {
            return ResponseEntity.badRequest().body(result.getError());
        }
    }

    @GetMapping("/{messageId}")
    public ResponseEntity<?> getMessage(@PathVariable @ValidId long chatId, @PathVariable @ValidId long messageId, @CurrentUserId long userId) {

        ResultOneArg<UserMessageDTO> result = messageService.getMessage(chatId, userId, messageId);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getResult());
        } else {
            return ResponseEntity.badRequest().body(result.getError());
        }
    }

    @GetMapping
    public ResponseEntity<?> getMessagesPage(@PathVariable @ValidId long chatId, @Valid PaginationRequest pagination,
                                             @RequestParam(defaultValue = "BACKWARD") @NotNull Direction direction, @CurrentUserId long userId) {

        ResultOneArg<MessagesPageDTO> result = messageService.getMessagePagination(chatId, userId, pagination.getCursor(), pagination.getLimit(), direction);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getResult());
        } else {
            return ResponseEntity.badRequest().body(result.getError());
        }
    }
}
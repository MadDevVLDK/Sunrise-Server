package com.sunrise.web.api;

import com.sunrise.web.api.annotation.CurrentUserId;
import com.sunrise.web.api.annotation.ValidId;

import com.sunrise.web.api.request.PaginationRequest;
import com.sunrise.web.api.request.PrivateMessageRequest;
import com.sunrise.web.api.request.PublicMessageRequest;
import com.sunrise.web.api.request.UpdateMessageRequest;
import com.sunrise.web.api.response.ApiResponse;
import com.sunrise.service.result.*;

import com.sunrise.dataservice.type.Direction;
import com.sunrise.service.MessageService;

import com.sunrise.dataservice.result.UserMessageDTO;
import com.sunrise.dataservice.result.MessageReadStatusDTO;
import com.sunrise.dataservice.result.MessagesPageDTO;
import jakarta.validation.Valid;

import jakarta.validation.constraints.NotNull;
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
    public ResponseEntity<?> sendPublicMessage(@PathVariable @ValidId long chatId,
                                               @RequestBody @Valid PublicMessageRequest request, @CurrentUserId long userId) {

        ResultOneArg<Long> result = messageService.makePublicMessage(request.getTempId(), chatId, userId, request.getText());

        return result.isSuccess() ?
                ApiResponse.success(result.getResult()) :
                ApiResponse.error(result.getError());
    }

    @PostMapping("/private")
    public ResponseEntity<?> sendPrivateMessage(@PathVariable @ValidId long chatId,
                                                @RequestBody @Valid PrivateMessageRequest request, @CurrentUserId long userId) {

        ResultOneArg<Long> result = messageService.makePrivateMessage(request.getTempId(), chatId, userId, request.getUserToSendId(), request.getText());

        return result.isSuccess() ?
                ApiResponse.success(result.getResult()) :
                ApiResponse.error(result.getError());
    }


    @PutMapping("/{messageId}")
    public ResponseEntity<?> updateMessage(@PathVariable @ValidId long chatId, @PathVariable @ValidId long messageId,
                                           @RequestBody @Valid UpdateMessageRequest request, @CurrentUserId long userId) {

        ResultNoArgs result = messageService.updateMessage(chatId, userId, messageId, request.getText().trim());

        return result.isSuccess() ?
                ApiResponse.success() :
                ApiResponse.error(result.getError());
    }

    @PutMapping("/{messageId}/mark-up-to-read")
    public ResponseEntity<?> markMessagesUpToRead(@PathVariable @ValidId long chatId, @PathVariable @ValidId long messageId, @CurrentUserId long userId) {

        ResultNoArgs result = messageService.markMessagesUpToRead(chatId, userId, messageId);

        return result.isSuccess() ?
                ApiResponse.success() :
                ApiResponse.error(result.getError());
    }


    @DeleteMapping("/{messageId}")
    public ResponseEntity<?> deleteMessage(@PathVariable @ValidId long chatId, @PathVariable @ValidId long messageId, @CurrentUserId long userId) {

        ResultNoArgs result = messageService.deleteMessage(chatId, userId, messageId);

        return result.isSuccess() ?
                ApiResponse.success() :
                ApiResponse.error(result.getError());
    }


    @GetMapping("/{messageId}/reads")
    public ResponseEntity<?> getMessageReads(@PathVariable @ValidId long chatId, @PathVariable @ValidId long messageId, @CurrentUserId long userId) {

        ResultOneArg<List<MessageReadStatusDTO>> result = messageService.getMessageReads(chatId, userId, messageId);

        return result.isSuccess() ?
                ApiResponse.success(result.getResult()) :
                ApiResponse.error(result.getError());
    }

    @GetMapping("/{messageId}")
    public ResponseEntity<?> getMessage(@PathVariable @ValidId long chatId, @PathVariable @ValidId long messageId, @CurrentUserId long userId) {

        ResultOneArg<UserMessageDTO> result = messageService.getMessage(chatId, userId, messageId);

        return result.isSuccess() ?
                ApiResponse.success(result.getResult()) :
                ApiResponse.error(result.getError());
    }

    @GetMapping
    public ResponseEntity<?> getMessagesPage(@PathVariable @ValidId long chatId, @Valid PaginationRequest pagination,
                                             @RequestParam(defaultValue = "BACKWARD") @NotNull Direction direction, @CurrentUserId long userId) {

        ResultOneArg<MessagesPageDTO> result = messageService.getMessagePagination(chatId, userId, pagination.getCursor(), pagination.getLimit(), direction);

        return result.isSuccess() ?
                ApiResponse.success(result.getResult()) :
                ApiResponse.error(result.getError());
    }
}
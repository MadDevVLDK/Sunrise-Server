package com.sunrise.web.api.controller;

import com.sunrise.core.service.ChatService;
import com.sunrise.orchestrator.result.Dto.*;
import com.sunrise.web.api.annotation.CurrentUserId;
import com.sunrise.web.api.annotation.ValidId;
import com.sunrise.web.payload.ApiRequest.Batch;
import com.sunrise.web.payload.ApiRequest.ChatSyncRequest;
import com.sunrise.web.payload.ApiRequest.CreateGroupChat;
import com.sunrise.web.payload.ApiRequest.CreatePersonalChat;
import com.sunrise.web.payload.ApiRequest.UpdateChatInfo;
import com.sunrise.web.payload.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/chats")
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/create-personal")
    public ResponseEntity<?> createPersonalChat(@RequestBody @Valid CreatePersonalChat request, @CurrentUserId long userId) {
        Long chatId = chatService.createPersonalChat(request.tempId(), userId, request.otherUserId());
        return ApiResponse.success(chatId);
    }

    @PostMapping("/create-group")
    public ResponseEntity<?> createGroupChat(@RequestBody @Valid CreateGroupChat request, @CurrentUserId long userId) {
        Long chatId = chatService.createGroupChat(
            request.tempId(), userId,
            request.chatName().trim(),
            request.chatDescription(),
            request.members()
        );
        return ApiResponse.success(chatId);
    }

    @PutMapping("/{chatId}/info")
    public ResponseEntity<?> updateChatInfo(@PathVariable("chatId") @ValidId long chatId,
                                            @RequestBody @Valid UpdateChatInfo request, @CurrentUserId long userId) {
        chatService.updateChatInfo(chatId, userId, request.chatName(), request.chatDescription());
        return ApiResponse.success();
    }

    @DeleteMapping("/{chatId}")
    public ResponseEntity<?> deleteChat(@PathVariable("chatId") @ValidId long chatId, @CurrentUserId long userId) {
        chatService.deleteChat(chatId, userId);
        return ApiResponse.success();
    }

    @GetMapping("/{chatId}/stats")
    public ResponseEntity<?> getChatStats(@PathVariable("chatId") @ValidId long chatId, @CurrentUserId long userId) {
        ChatStatsResult result = chatService.getChatStats(chatId, userId);
        return ApiResponse.success(result);
    }

    @GetMapping("/meta")
    public ResponseEntity<?> getUserChatsMeta(@CurrentUserId long userId) {
        List<ChatMeta> result = chatService.getUserChatsMeta(userId);
        return ApiResponse.success(result);
    }

    @GetMapping("/{chatId}")
    public ResponseEntity<?> getUserChat(@PathVariable("chatId") @ValidId long chatId, @CurrentUserId long userId) {
        ChatProfile result = chatService.getUserChat(chatId, userId);
        return ApiResponse.success(result);
    }

    @GetMapping("/batch")
    public ResponseEntity<?> getUserChatsByIds(@Valid Batch request, @CurrentUserId long userId) {
        List<ChatProfile> result = chatService.getUserChatsByIds(userId, request.ids());
        return ApiResponse.success(result);
    }

    @PostMapping("/sync")
    public ResponseEntity<?> syncChats(@RequestBody @Valid ChatSyncRequest request, @CurrentUserId long userId) {
        Map<Long, ChatEventSync> result = chatService.syncChats(userId, request.cursors());
        return ApiResponse.success(result);
    }
}
package com.sunrise.web.api;

import com.sunrise.web.api.annotation.CurrentUserId;
import com.sunrise.web.payload.ApiRequest;
import com.sunrise.web.payload.ApiRequest.Batch;
import com.sunrise.web.payload.ApiRequest.CreateGroupChat;
import com.sunrise.web.payload.ApiRequest.CreatePersonalChat;
import com.sunrise.web.payload.ApiRequest.SyncRequest;
import com.sunrise.web.payload.ApiResponse;
import com.sunrise.db.event.ChatEvent;
import com.sunrise.db.result.ChatStatsResult;
import com.sunrise.orchestrator.result.ChatMetaDTO;
import com.sunrise.orchestrator.result.ChatProfileDTO;
import com.sunrise.web.api.annotation.ValidId;
import com.sunrise.core.result.*;
import com.sunrise.core.service.ChatService;

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

        ResultOneArg<Long> result = chatService.createPersonalChat(request.tempId(), userId, request.otherUserId());

        return result.isSuccess() ?
                ApiResponse.success(result.getResult()) :
                ApiResponse.error(result.getError());
    }

    @PostMapping("/create-group")
    public ResponseEntity<?> createGroupChat(@RequestBody @Valid CreateGroupChat request, @CurrentUserId long userId) {

        ResultOneArg<Long> result = chatService.createGroupChat(
            request.tempId(), userId,
            request.chatName().trim(),
            request.chatDescription(),
            request.members()
        );

        return result.isSuccess() ?
                ApiResponse.success(result.getResult()) :
                ApiResponse.error(result.getError());
    }

    @PutMapping("/{chatId}/info")
    public ResponseEntity<?> updateChatInfo(@PathVariable("chatId") @ValidId long chatId, 
                                            @RequestBody @Valid ApiRequest.UpdateChatInfo request, @CurrentUserId long userId) {

        ResultNoArgs result = chatService.updateChatInfo(chatId, userId, request.chatName(), request.chatDescription());

        return result.isSuccess() ?
                ApiResponse.success() :
                ApiResponse.error(result.getError());
    }

    @DeleteMapping("/{chatId}")
    public ResponseEntity<?> deleteChat(@PathVariable("chatId") @ValidId long chatId, @CurrentUserId long userId) {

        ResultNoArgs result = chatService.deleteChat(chatId, userId);

        return result.isSuccess() ?
                ApiResponse.success() :
                ApiResponse.error(result.getError());
    }

    @GetMapping("/{chatId}/stats")
    public ResponseEntity<?> getChatStats(@PathVariable("chatId") @ValidId long chatId, @CurrentUserId long userId) {

        ResultOneArg<ChatStatsResult> result = chatService.getChatStats(chatId, userId);

        return result.isSuccess() ?
                ApiResponse.success(result.getResult()) :
                ApiResponse.error(result.getError());
    }

    @GetMapping("/meta")
    public ResponseEntity<?> getUserChatsMeta(@CurrentUserId long userId) {
        ResultOneArg<List<ChatMetaDTO>> result = chatService.getUserChatsMeta(userId);

        return result.isSuccess() ?
                ApiResponse.success(result.getResult()) :
                ApiResponse.error(result.getError());
    }

    @GetMapping("/{chatId}")
    public ResponseEntity<?> getUserChat(@PathVariable("chatId") @ValidId long chatId, @CurrentUserId long userId) {

        ResultOneArg<ChatProfileDTO> result = chatService.getUserChat(chatId, userId);

        return result.isSuccess() ?
                ApiResponse.success(result.getResult()) :
                ApiResponse.error(result.getError());
    }

    @GetMapping("/batch")
    public ResponseEntity<?> getUserChatsByIds(@Valid Batch request, @CurrentUserId long userId) {

        ResultOneArg<List<ChatProfileDTO>> result = chatService.getUserChatsByIds(userId, request.ids());

        return result.isSuccess() ?
                ApiResponse.success(result.getResult()) :
                ApiResponse.error(result.getError());
    }

    @PostMapping("/sync")
    public ResponseEntity<?> syncChats(@RequestBody SyncRequest request, @CurrentUserId long userId) {

        ResultOneArg<Map<Long, ChatSyncDTO>> result = chatService.syncChats(userId, request.chatSeqs());

        return result.isSuccess() ?
                ApiResponse.success(result.getResult()) :
                ApiResponse.error(result.getError());
    }

    public record ChatEventDTO(long seq, String type, ChatEvent.IChatEvent event) {}
    public record ChatSyncDTO(List<ChatEventDTO> events, boolean hasMore) {}
}
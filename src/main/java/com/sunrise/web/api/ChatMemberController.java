package com.sunrise.web.api;

import com.sunrise.web.api.annotation.CurrentUserId;
import com.sunrise.web.api.annotation.ValidId;
import com.sunrise.web.payload.ApiRequest;
import com.sunrise.web.payload.ApiResponse;
import com.sunrise.core.result.ResultNoArgs;
import com.sunrise.core.result.ResultOneArg;
import com.sunrise.core.service.ChatMemberService;
import com.sunrise.orchestrator.result.ChatMemberProfileDTO;
import com.sunrise.orchestrator.result.ChatMembersPageDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/chats/{chatId}/members")
public class ChatMemberController {

    private final ChatMemberService chatMemberService;

    
    @PostMapping("/add")
    public ResponseEntity<?> addGroupMember(@PathVariable("chatId") @ValidId long chatId, 
                                            @RequestBody @Valid ApiRequest.AddGroupMember request, @CurrentUserId long userId) {

        ResultNoArgs result = chatMemberService.addOrRestoreChatMember(chatId, userId, request.newUserId());

        return result.isSuccess() ?
                ApiResponse.success() :
                ApiResponse.error(result.getError());
    }

    @PostMapping("/add-many")
    public ResponseEntity<?> addGroupMembers(@PathVariable("chatId") @ValidId long chatId, 
                                             @RequestBody @Valid ApiRequest.AddGroupMembers request, @CurrentUserId long userId) {

        ResultNoArgs result = chatMemberService.addOrRestoreChatMembers(chatId, userId, request.members());

        return result.isSuccess() ?
                ApiResponse.success() :
                ApiResponse.error(result.getError());
    }

    @PutMapping("/{otherUserId}/info")
    public ResponseEntity<?> updateChatMemberInfo(@PathVariable("chatId") @ValidId long chatId, @PathVariable("otherUserId") @ValidId long otherUserId, 
                                                  @RequestBody @Valid ApiRequest.UpdateChatMemberInfo request, @CurrentUserId long userId) {
        
        ResultNoArgs result = chatMemberService.updateChatMemberInfo(chatId, userId, otherUserId, request.tag());

        return result.isSuccess() ?
                ApiResponse.success() :
                ApiResponse.error(result.getError());
    }

    @PutMapping("/{otherUserId}/admin-rights")
    public ResponseEntity<?> updateAdminRights(@PathVariable("chatId") @ValidId long chatId, @PathVariable("otherUserId") @ValidId long otherUserId, 
                                               @RequestBody @Valid ApiRequest.UpdateAdminRights request, @CurrentUserId long userId) {
        
        ResultNoArgs result = chatMemberService.updateChatMemberAdminRight(chatId, userId, otherUserId, request.isAdmin());

        return result.isSuccess() ?
                ApiResponse.success() :
                ApiResponse.error(result.getError());
    }

    @PutMapping("/self")
    public ResponseEntity<?> updateSelfChatSettings(@PathVariable("chatId") @ValidId long chatId, 
                                                    @RequestBody @Valid ApiRequest.UpdateSelfChatSettings request, @CurrentUserId long userId) {
    
        ResultNoArgs result = chatMemberService.updateSelfChatSettings(chatId, userId, request.isPinned());

        return result.isSuccess() ?
                ApiResponse.success() :
                ApiResponse.error(result.getError());
    }

    @DeleteMapping("/{otherUserId}/kick")
    public ResponseEntity<?> kickChatMember(@PathVariable("chatId") @ValidId long chatId, 
                                            @PathVariable("otherUserId") @ValidId long otherUserId, @CurrentUserId long userId) {
        
        ResultNoArgs result = chatMemberService.kickChatMember(chatId, userId, otherUserId);

        return result.isSuccess() ?
                ApiResponse.success() :
                ApiResponse.error(result.getError());
    }

    @DeleteMapping("/leave")
    public ResponseEntity<?> leaveChat(@PathVariable("chatId") @ValidId long chatId, @CurrentUserId long userId) {
        
        ResultNoArgs result = chatMemberService.leaveChat(chatId, userId);

        return result.isSuccess() ?
                ApiResponse.success() :
                ApiResponse.error(result.getError());
    }

    @GetMapping
    public ResponseEntity<?> getChatMembersPage(@PathVariable("chatId") @ValidId long chatId, @Valid ApiRequest.ChatMemberPagination request, @CurrentUserId long userId) {

        ResultOneArg<ChatMembersPageDTO> result = chatMemberService.getChatMemberPage(chatId, userId, request.cursor(), request.getLimit());

        return result.isSuccess() ?
                ApiResponse.success(result.getResult()) :
                ApiResponse.error(result.getError());
    }
    
    @GetMapping("/batch")
    public ResponseEntity<?> getChatMembersPage(@PathVariable("chatId") @ValidId long chatId, @Valid ApiRequest.Batch request, 
                                                @CurrentUserId long userId) {

        ResultOneArg<List<ChatMemberProfileDTO>> result = chatMemberService.getChatMemberByIds(chatId, userId, request.ids());

        return result.isSuccess() ?
                ApiResponse.success(result.getResult()) :
                ApiResponse.error(result.getError());
    }
}

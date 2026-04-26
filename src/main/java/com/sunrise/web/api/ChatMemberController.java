package com.sunrise.web.api;

import com.sunrise.web.api.annotation.CurrentUserId;
import com.sunrise.web.api.annotation.ValidId;
import com.sunrise.web.api.request.*;
import com.sunrise.web.api.response.ApiResponse;
import com.sunrise.service.ChatMemberService;
import com.sunrise.service.result.ResultNoArgs;
import com.sunrise.service.result.ResultOneArg;
import com.sunrise.dataservice.result.ChatMemberProfileDTO;
import com.sunrise.dataservice.result.ChatMembersPageDTO;

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
    public ResponseEntity<?> addGroupMember(@PathVariable @ValidId long chatId, @RequestBody @Valid AddGroupMemberRequest request, @CurrentUserId long userId) {

        ResultNoArgs result = chatMemberService.addOrRestoreChatMember(chatId, userId, request.getNewUserId());

        return result.isSuccess() ?
                ApiResponse.success() :
                ApiResponse.error(result.getError());
    }

    @PostMapping("/add-many")
    public ResponseEntity<?> addGroupMembers(@PathVariable @ValidId long chatId, @RequestBody @Valid AddGroupMembersRequest request, @CurrentUserId long userId) {

        ResultNoArgs result = chatMemberService.addOrRestoreChatMembers(chatId, userId, request.getMembers());

        return result.isSuccess() ?
                ApiResponse.success() :
                ApiResponse.error(result.getError());
    }

    @PutMapping("/{otherUserId}/info")
    public ResponseEntity<?> updateChatMemberInfo(@PathVariable @ValidId long chatId, @PathVariable @ValidId long otherUserId, @RequestBody @Valid UpdateChatMemberInfoRequest request, @CurrentUserId long userId) {
        ResultNoArgs result = chatMemberService.updateChatMemberInfo(chatId, userId, otherUserId, request.getTag());

        return result.isSuccess() ?
                ApiResponse.success() :
                ApiResponse.error(result.getError());
    }

    @PutMapping("/{otherUserId}/admin-rights")
    public ResponseEntity<?> updateAdminRights(@PathVariable @ValidId long chatId, @PathVariable @ValidId long otherUserId, @RequestBody @Valid UpdateAdminRightsRequest request, @CurrentUserId long userId) {
        ResultNoArgs result = chatMemberService.updateChatMemberAdminRight(chatId, userId, otherUserId, request.getIsAdmin());

        return result.isSuccess() ?
                ApiResponse.success() :
                ApiResponse.error(result.getError());
    }

    @PutMapping("/self")
    public ResponseEntity<?> updateSelfChatSettings(@PathVariable @ValidId long chatId, @RequestBody @Valid UpdateSelfChatSettingsRequest request, @CurrentUserId long userId) {
        ResultNoArgs result = chatMemberService.updateSelfChatSettings(chatId, userId, request.getIsPinned());

        return result.isSuccess() ?
                ApiResponse.success() :
                ApiResponse.error(result.getError());
    }

    @DeleteMapping("/{otherUserId}/kick")
    public ResponseEntity<?> kickChatMember(@PathVariable @ValidId long chatId, @PathVariable @ValidId long otherUserId, @CurrentUserId long userId) {
        ResultNoArgs result = chatMemberService.kickChatMember(chatId, userId, otherUserId);

        return result.isSuccess() ?
                ApiResponse.success() :
                ApiResponse.error(result.getError());
    }

    @DeleteMapping("/leave")
    public ResponseEntity<?> leaveChat(@PathVariable @ValidId long chatId, @CurrentUserId long userId) {
        ResultNoArgs result = chatMemberService.leaveChat(chatId, userId);

        return result.isSuccess() ?
                ApiResponse.success() :
                ApiResponse.error(result.getError());
    }

    @GetMapping
    public ResponseEntity<?> getChatMembersPage(@PathVariable @ValidId long chatId, @Valid PaginationRequest request, @CurrentUserId long userId) {

        ResultOneArg<ChatMembersPageDTO> result = chatMemberService.getChatMemberPage(chatId, userId, request.getCursor(), request.getLimit());

        return result.isSuccess() ?
                ApiResponse.success(result.getResult()) :
                ApiResponse.error(result.getError());
    }
    @GetMapping("/by-ids")
    public ResponseEntity<?> getChatMembersPage(@PathVariable @ValidId long chatId, @RequestBody @Valid GetChatMembersByIds request, @CurrentUserId long userId) {

        ResultOneArg<List<ChatMemberProfileDTO>> result = chatMemberService.getChatMemberByIds(chatId, userId, request.getMembers());

        return result.isSuccess() ?
                ApiResponse.success(result.getResult()) :
                ApiResponse.error(result.getError());
    }
}

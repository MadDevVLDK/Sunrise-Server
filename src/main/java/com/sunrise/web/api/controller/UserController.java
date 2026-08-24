package com.sunrise.web.api.controller;

import com.sunrise.core.service.UserService;
import com.sunrise.orchestrator.result.Dto.UserGlobalEventSync;
import com.sunrise.orchestrator.result.Dto.UsersPage;
import com.sunrise.web.api.annotation.CurrentUserId;
import com.sunrise.web.api.annotation.ValidId;
import com.sunrise.web.payload.ApiRequest;
import com.sunrise.web.payload.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<?> getActiveUsersPage(@Valid ApiRequest.UserPagination request,
                                                @CurrentUserId long userId) {
        UsersPage result = userService.getActiveUsersPage(userId, request.getFilter(), request.cursor(), request.getLimit());
        return ApiResponse.success(result);
    }

    @PostMapping("/sync")
    public ResponseEntity<?> syncUserEvents(@RequestParam("cursor") @ValidId long cursor,
                                            @CurrentUserId long userId) {
        UserGlobalEventSync result = userService.syncUserEvents(userId, cursor);
        return ApiResponse.success(result);
    }
}
package com.sunrise.web.api;

import com.sunrise.web.api.annotation.CurrentUserId;
import com.sunrise.web.payload.ApiRequest;
import com.sunrise.web.payload.ApiResponse;
import com.sunrise.core.result.ResultOneArg;
import com.sunrise.core.service.UserService;
import com.sunrise.orchestrator.result.Dto.UsersPage;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("api/users")
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<?> getActiveUsersPage(@Valid ApiRequest.UserPagination request, @CurrentUserId long userId) {

        ResultOneArg<UsersPage> result = userService.getActiveUsersPage(userId, request.getFilter(), request.cursor(), request.getLimit());

        return result.isSuccess() ?
                ApiResponse.success(result.getResult()) :
                ApiResponse.error(result.getError());
    }
}

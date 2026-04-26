package com.sunrise.web.api;

import com.sunrise.web.api.annotation.CurrentUserId;
import com.sunrise.web.api.response.ApiResponse;
import com.sunrise.service.UserService;

import com.sunrise.web.api.request.PaginationRequest;
import com.sunrise.service.result.ResultOneArg;
import com.sunrise.dataservice.result.UsersPageDTO;
import jakarta.validation.Valid;

import jakarta.validation.constraints.NotNull;
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
    public ResponseEntity<?> getActiveUsersPage(@RequestParam(defaultValue = "") @NotNull String filter, @Valid PaginationRequest pagination,
                                                @CurrentUserId long userId) {

        ResultOneArg<UsersPageDTO> result = userService.getActiveUsersPage(userId, filter, pagination.getCursor(), pagination.getLimit());

        return result.isSuccess() ?
                ApiResponse.success(result.getResult()) :
                ApiResponse.error(result.getError());
    }
}

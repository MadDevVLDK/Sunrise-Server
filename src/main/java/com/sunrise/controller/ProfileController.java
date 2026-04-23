package com.sunrise.controller;

import com.sunrise.config.annotation.CurrentUserId;
import com.sunrise.config.annotation.ValidId;
import com.sunrise.controller.response.ApiResponse;
import com.sunrise.core.service.UserService;
import com.sunrise.controller.request.ProfileUpdateRequest;
import com.sunrise.core.service.result.*;
import com.sunrise.entity.dto.UserProfileFullDTO;
import com.sunrise.entity.dto.UserProfileLightDTO;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/profile")
public class ProfileController {
    private final UserService userService;

    @PutMapping
    public ResponseEntity<?> updateProfile(@RequestBody @Valid ProfileUpdateRequest request, @CurrentUserId long userId) {

        ResultNoArgs result = userService.updateProfile(userId, request.getUsername(), request.getName());

        return result.isSuccess() ?
                ApiResponse.success() :
                ApiResponse.error(result.getError());
    }
    @DeleteMapping
    public ResponseEntity<?> deleteProfile(@CurrentUserId long userId) {

        ResultNoArgs result = userService.deleteUser(userId, userId);

        return result.isSuccess() ?
                ApiResponse.success() :
                ApiResponse.error(result.getError());
    }

    @GetMapping
    public ResponseEntity<?> getMyProfile(@CurrentUserId long userId) {

        ResultOneArg<UserProfileLightDTO> result = userService.getMyProfile(userId);

        return result.isSuccess() ?
                ApiResponse.success(result.getResult()) :
                ApiResponse.error(result.getError());
    }

    @GetMapping("/{otherUserId}/light")
    public ResponseEntity<?> getOtherProfileLight(@PathVariable @ValidId long otherUserId, @CurrentUserId long userId) {

        ResultOneArg<UserProfileLightDTO> result = userService.getOtherProfileLight(userId, otherUserId);

        return result.isSuccess() ?
                ApiResponse.success(result.getResult()) :
                ApiResponse.error(result.getError());
    }

    @GetMapping("/{otherUserId}")
    public ResponseEntity<?> getOtherProfileFull(@PathVariable @ValidId long otherUserId, @CurrentUserId long userId) {

        ResultOneArg<UserProfileFullDTO> result = userService.getOtherProfileFull(userId, otherUserId);

        return result.isSuccess() ?
                ApiResponse.success(result.getResult()) :
                ApiResponse.error(result.getError());
    }
}

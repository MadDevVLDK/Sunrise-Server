package com.sunrise.web.api.controller;

import com.sunrise.core.service.UserService;
import com.sunrise.orchestrator.result.Dto.*;
import com.sunrise.web.api.annotation.CurrentUserId;
import com.sunrise.web.api.annotation.ValidId;
import com.sunrise.web.payload.ApiRequest;
import com.sunrise.web.payload.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    private final UserService userService;

    @PutMapping
    public ResponseEntity<?> updateProfile(@RequestBody @Valid ApiRequest.ProfileUpdate request, @CurrentUserId long userId) {
        userService.updateProfile(userId, request.username(), request.name());
        return ApiResponse.success();
    }

    @DeleteMapping
    public ResponseEntity<?> deleteProfile(@CurrentUserId long userId) {
        userService.deleteUser(userId, userId);
        return ApiResponse.success();
    }

    @GetMapping
    public ResponseEntity<?> getMyProfile(@CurrentUserId long userId) {
        UserProfileLight result = userService.getMyProfile(userId);
        return ApiResponse.success(result);
    }

    @GetMapping("/{otherUserId}")
    public ResponseEntity<?> getOtherProfile(@PathVariable("otherUserId") @ValidId long otherUserId,
                                             @RequestParam(value = "light", required = false) Boolean light,
                                             @CurrentUserId long userId) {
        if (Boolean.TRUE.equals(light)) {
            UserProfileLight result = userService.getOtherProfileLight(userId, otherUserId);
            return ApiResponse.success(result);
        } else {
            UserProfileFull result = userService.getOtherProfileFull(userId, otherUserId);
            return ApiResponse.success(result);
        }
    }

    @GetMapping("/batch")
    public ResponseEntity<?> getOtherProfilesByIds(@Valid ApiRequest.Batch request, @CurrentUserId long userId) {
        List<UserProfileLight> result = userService.getOtherProfileLightByIds(userId, request.ids());
        return ApiResponse.success(result);
    }
}
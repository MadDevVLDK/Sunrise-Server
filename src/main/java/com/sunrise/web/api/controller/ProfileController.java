package com.sunrise.web.api.controller;

import com.sunrise.web.api.annotation.CurrentUserId;
import com.sunrise.web.api.annotation.ValidId;
import com.sunrise.web.payload.ApiRequest;
import com.sunrise.web.payload.ApiResponse;
import com.sunrise.core.result.*;
import com.sunrise.core.service.UserService;
import com.sunrise.orchestrator.result.Dto.*;

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

        ResultNoArgs result = userService.updateProfile(userId, request.username(), request.name());

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

        ResultOneArg<UserProfileLight> result = userService.getMyProfile(userId);

        return result.isSuccess() ?
                ApiResponse.success(result.getResult()) :
                ApiResponse.error(result.getError());
    }

    @GetMapping("/{otherUserId}")
    public ResponseEntity<?> getOtherProfile(@PathVariable("otherUserId") @ValidId long otherUserId, 
                                             @RequestParam("light") Boolean light, @CurrentUserId long userId) {

        ResultOneArg<?> result;
        if (light == null) {
            result = userService.getOtherProfileFull(userId, otherUserId);
        } else if (light) {
            result = userService.getOtherProfileLight(userId, otherUserId);
        } else {
            result = userService.getOtherProfileFull(userId, otherUserId);
        }

        return result.isSuccess() ?
                ApiResponse.success(result.getResult()) :
                ApiResponse.error(result.getError());
    }

    @GetMapping("/batch")
    public ResponseEntity<?> getOtherProfilesByIds(@Valid ApiRequest.Batch request, @CurrentUserId long userId) {

        ResultOneArg<List<UserProfileLight>> result = userService.getOtherProfileLightByIds(userId, request.ids());

        return result.isSuccess() ?
                ApiResponse.success(result.getResult()) :
                ApiResponse.error(result.getError());
    }
}

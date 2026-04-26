package com.sunrise.web.api;

import com.sunrise.web.api.annotation.CurrentUserId;
import com.sunrise.web.api.annotation.ValidId;
import com.sunrise.web.api.request.GetUserProfilesByIds;
import com.sunrise.web.api.response.ApiResponse;
import com.sunrise.service.UserService;
import com.sunrise.web.api.request.ProfileUpdateRequest;
import com.sunrise.service.result.*;
import com.sunrise.dataservice.result.UserProfileLightDTO;

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

    @GetMapping("/{otherUserId}")
    public ResponseEntity<?> getOtherProfile(@PathVariable @ValidId long otherUserId, @RequestParam Boolean light, @CurrentUserId long userId) {

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

    @GetMapping("/by-ids")
    public ResponseEntity<?> getOtherProfilesByIds(@RequestBody @Valid GetUserProfilesByIds request, @CurrentUserId long userId) {

        ResultOneArg<List<UserProfileLightDTO>> result = userService.getOtherProfileLightByIds(userId, request.getUserIds());

        return result.isSuccess() ?
                ApiResponse.success(result.getResult()) :
                ApiResponse.error(result.getError());
    }
}

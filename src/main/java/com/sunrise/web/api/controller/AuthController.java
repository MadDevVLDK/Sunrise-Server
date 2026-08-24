package com.sunrise.web.api.controller;

import com.sunrise.core.service.AuthService;
import com.sunrise.orchestrator.result.Dto;
import com.sunrise.web.api.annotation.CurrentUserId;
import com.sunrise.web.payload.ApiRequest;
import com.sunrise.web.payload.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid ApiRequest.Register request) {
        String message = authService.registerUser(
            request.username().trim(),
            request.name().trim(),
            request.email().trim(),
            request.password().trim()
        );
        return ApiResponse.success(message);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid ApiRequest.Login request, HttpServletRequest httpRequest) {
        Dto.UserLogin result = authService.authenticateUser(
            request.username().trim(),
            request.password().trim(),
            httpRequest
        );
        return ApiResponse.success(result);
    }

    @PutMapping("/change-email")
    public ResponseEntity<?> requestEmailChange(@CurrentUserId long userId) {
        authService.requestEmailUpdate(userId);
        return ApiResponse.success("Confirmation sent to your current email address");
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> requestPasswordChange(@RequestParam("email") @NotBlank @Email String email) {
        authService.requestPasswordUpdate(email);
        return ApiResponse.success("If the user exists, a reset link has been sent");
    }
}
package com.sunrise.controller;

import com.sunrise.config.annotation.CurrentUserId;
import com.sunrise.controller.request.LoginRequest;
import com.sunrise.controller.request.RegisterRequest;
import com.sunrise.core.service.result.*;
import com.sunrise.core.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterRequest request) {

        ResultOneArg<String> result = authService.registerUser(
            request.getUsername().trim(),
            request.getName().trim(),
            request.getEmail().trim(),
            request.getPassword().trim()
        );

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getResult());
        } else {
            return ResponseEntity.badRequest().body(result.getError());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest request, HttpServletRequest httpRequest) {

        ResultOneArg<UserLoginResult> result = authService.authenticateUser(
            request.getUsername().trim(),
            request.getPassword().trim(),
            httpRequest
        );

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getResult());
        } else {
            return ResponseEntity.badRequest().body(result.getError());
        }
    }

    @PutMapping("/change-email")
    public ResponseEntity<?> requestEmailChange(@CurrentUserId long userId) {
        ResultNoArgs result = authService.requestEmailUpdate(userId);
        if (result.isSuccess()) {
            return ResponseEntity.ok("Confirmation sent to your current email address");
        } else {
            return ResponseEntity.badRequest().body(result.getError());
        }
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> requestPasswordChange(@RequestParam @NotBlank @Email String email) {
        ResultNoArgs result = authService.requestPasswordUpdate(email);
        return ResponseEntity.ok("If the user exists, a reset link has been sent");
    }
}
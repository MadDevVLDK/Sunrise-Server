package com.sunrise.web.api;

import com.sunrise.web.api.annotation.CurrentUserId;
import com.sunrise.web.api.request.LoginRequest;
import com.sunrise.web.api.request.RegisterRequest;
import com.sunrise.web.api.response.ApiResponse;
import com.sunrise.service.result.*;
import com.sunrise.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
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

        return result.isSuccess() ?
                ApiResponse.success(result.getResult()) :
                ApiResponse.error(result.getError());
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest request, HttpServletRequest httpRequest) {

        ResultOneArg<UserLoginResult> result = authService.authenticateUser(
            request.getUsername().trim(),
            request.getPassword().trim(),
            httpRequest
        );

        return result.isSuccess() ?
                ApiResponse.success(result.getResult()) :
                ApiResponse.error(result.getError());
    }

    @PutMapping("/change-email")
    public ResponseEntity<?> requestEmailChange(@CurrentUserId long userId) {

        ResultNoArgs result = authService.requestEmailUpdate(userId);

        return result.isSuccess() ?
                ApiResponse.success("Confirmation sent to your current email address") :
                ApiResponse.error(result.getError());
    }

    @PutMapping("/change-password") // TODO: ТУТ ОШИБКА МОЖЕТ БЫТЬ БРОШЕНА
    public ResponseEntity<?> requestPasswordChange(@RequestParam @NotBlank @Email String email) {

        ResultNoArgs result = authService.requestPasswordUpdate(email);

        return result.isSuccess() ?
                ApiResponse.success("If the user exists, a reset link has been sent") :
                ApiResponse.error("Some error occurred while processing your request");
    }
}
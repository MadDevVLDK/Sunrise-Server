package com.sunrise.web.api;

import com.sunrise.core.result.*;
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
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid ApiRequest.Register request) {

        ResultOneArg<String> result = authService.registerUser(
            request.username().trim(),
            request.name().trim(),
            request.email().trim(),
            request.password().trim()
        );

        return result.isSuccess() ?
                ApiResponse.success(result.getResult()) :
                ApiResponse.error(result.getError());
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid ApiRequest.Login request, HttpServletRequest httpRequest) {

        ResultOneArg<Dto.UserLogin> result = authService.authenticateUser(
            request.username().trim(),
            request.password().trim(),
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
    public ResponseEntity<?> requestPasswordChange(@RequestParam("email") @NotBlank @Email String email) {

        ResultNoArgs result = authService.requestPasswordUpdate(email);

        return result.isSuccess() ?
                ApiResponse.success("If the user exists, a reset link has been sent") :
                ApiResponse.error("Some error occurred while processing your request");
    }
}
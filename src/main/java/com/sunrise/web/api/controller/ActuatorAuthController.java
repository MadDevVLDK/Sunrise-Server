package com.sunrise.web.api.controller;

import com.sunrise.core.result.ResultOneArg;
import com.sunrise.core.service.AuthService;
import com.sunrise.web.payload.ApiResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

@RequiredArgsConstructor
@RestController
@RequestMapping("/actuator/auth")
public class ActuatorAuthController {

    private final AuthService authService;

    @GetMapping("/token")
    public ResponseEntity<?> getActuatorToken(HttpServletRequest httpRequest) {
        try {
            ResultOneArg<String> result = authService.getActuatorToken(httpRequest);
            
            return result.isSuccess() ?
                    ApiResponse.success(result.getResult()) :
                    ApiResponse.error(result.getError());
        } catch (Exception e) {
            return ApiResponse.error("Ошибка при генерации токена: " + e.getMessage());
        }
    }
}
package com.sunrise.web.api.controller;

import com.sunrise.core.service.AuthService;
import com.sunrise.helpclass.exception.MyException;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping("/forms/auth-confirmation/{token}")
public class ConfirmationController {

    private final AuthService authService;

    @GetMapping("/reg")
    public String confirmRegistration(@PathVariable("token") @Size(min = 64, max = 64) String token, Model model) {
        try {
            String message = authService.confirmRegistrationToken(token);
            model.addAttribute("isSuccess", true);
            model.addAttribute("message", message);
        } catch (MyException e) {
            log.warn("[📝] Registration confirmation failed: code={}, message={}", e.getCode(), e.getMessage());
            model.addAttribute("isSuccess", false);
            model.addAttribute("message", e.getMessage());
        }
        return "confirm-registration";
    }

    @GetMapping("/email")
    public String showEmailUpdateForm(@PathVariable("token") @Size(min = 64, max = 64) String token, Model model) {
        model.addAttribute("token", token);
        model.addAttribute("submitted", false);
        return "confirm-email-update";
    }

    @PostMapping("/email")
    public String confirmEmailUpdate(@PathVariable("token") @Size(min = 64, max = 64) String token,
                                     @RequestParam("email") @NotBlank @Email String email,
                                     Model model) {
        try {
            String message = authService.confirmEmailUpdateToken(token, email);
            model.addAttribute("isSuccess", true);
            model.addAttribute("message", message);
        } catch (MyException e) {
            log.warn("[📝] Email update confirmation failed: code={}, message={}", e.getCode(), e.getMessage());
            model.addAttribute("isSuccess", false);
            model.addAttribute("message", e.getMessage());
        }
        model.addAttribute("submitted", true);
        return "confirm-email-update";
    }

    @GetMapping("/password")
    public String showPasswordUpdateForm(@PathVariable("token") @Size(min = 64, max = 64) String token, Model model) {
        model.addAttribute("token", token);
        model.addAttribute("submitted", false);
        return "confirm-password-update";
    }

    @PostMapping("/password")
    public String confirmPasswordUpdate(@PathVariable("token") @Size(min = 64, max = 64) String token,
                                        @RequestParam("password") @NotBlank @Size(min = 8, max = 30) String password,
                                        Model model) {
        try {
            String message = authService.confirmPasswordUpdateToken(token, password);
            model.addAttribute("isSuccess", true);
            model.addAttribute("message", message);
        } catch (MyException e) {
            log.warn("[📝] Password update confirmation failed: code={}, message={}", e.getCode(), e.getMessage());
            model.addAttribute("isSuccess", false);
            model.addAttribute("message", e.getMessage());
        }
        model.addAttribute("submitted", true);
        return "confirm-password-update";
    }
}
package com.sunrise.web.api;

import com.sunrise.service.AuthService;
import com.sunrise.service.result.ResultOneArg;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@Controller // Не менять, потому что html работать не будет
@RequestMapping("/forms/auth-confirmation/{token}")
public class ConfirmationController {

    private final AuthService authService;

    @GetMapping("/reg")
    public String confirmRegistration(@PathVariable @Size(min = 64, max = 64) String token, Model model) {

        ResultOneArg<String> result = authService.confirmRegistrationToken(token);

        model.addAttribute("isSuccess", result.isSuccess());
        model.addAttribute("message", result.getResult());

        return "confirm-registration";
    }

    @GetMapping("/email")
    public String showEmailUpdateForm(@PathVariable @Size(min = 64, max = 64) String token, Model model) {
        model.addAttribute("token", token);
        model.addAttribute("submitted", false);
        return "confirm-email-update";
    }
    @PostMapping("/email")
    public String confirmEmailUpdate(@PathVariable @Size(min = 64, max = 64) String token,
                                     @RequestParam @NotBlank @Email String email,
                                     Model model) {
        ResultOneArg<String> result = authService.confirmEmailUpdateToken(token, email);
        model.addAttribute("isSuccess", result.isSuccess());
        model.addAttribute("message", result.getResult());
        model.addAttribute("submitted", true);
        return "confirm-email-update";
    }

    @GetMapping("/password")
    public String showPasswordUpdateForm(@PathVariable @Size(min = 64, max = 64) String token, Model model) {
        model.addAttribute("token", token);
        model.addAttribute("submitted", false);
        return "confirm-password-update";
    }
    @PostMapping("/password")
    public String confirmPasswordUpdate(@PathVariable @Size(min = 64, max = 64) String token,
                                        @RequestParam @NotBlank @Size(min = 8, max = 30) String password,
                                        Model model) {
        ResultOneArg<String> result = authService.confirmPasswordUpdateToken(token, password);
        model.addAttribute("isSuccess", result.isSuccess());
        model.addAttribute("message", result.getResult());
        model.addAttribute("submitted", true);
        return "confirm-password-update";
    }
}
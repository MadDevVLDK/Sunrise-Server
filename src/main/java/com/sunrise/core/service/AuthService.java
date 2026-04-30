package com.sunrise.core.service;

import com.sunrise.notifier.AsyncEvent;
import com.sunrise.core.creation.CreateDto;
import com.sunrise.core.result.*;
import com.sunrise.notifier.EmailNotifier;
import com.sunrise.orchestrator.DataValidator;
import com.sunrise.orchestrator.result.Dto;
import com.sunrise.orchestrator.result.Dto.*;
import com.sunrise.orchestrator.service.UserOrchestrator;
import com.sunrise.orchestrator.service.VerificationTokenOrchestrator;
import com.sunrise.orchestrator.type.TokenType;
import com.sunrise.web.jwt.JwtUtil;
import com.sunrise.helpclass.SimpleSnowflakeId;
import com.sunrise.helpclass.ValidationException;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.NonNull;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;


@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final ApplicationEventPublisher eventPublisher;
    private final EmailNotifier emailNotifier;

    private final UserOrchestrator userOrchestrator;
    private final VerificationTokenOrchestrator verificationTokenOrchestrator;

    private final DataValidator validator;

    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;


    @Transactional
    public ResultOneArg<String> registerUser(@NonNull String username, @NonNull String name, @NonNull String email, @NonNull String password) {
        try {
            if (userOrchestrator.existsUserByUsername(username.trim())) {
                throw new ValidationException("Username already exists");
            }

            if (userOrchestrator.existsUserByEmail(email.toLowerCase())) {
                throw new ValidationException("Email already exists");
            }

            Instant createdAt = Instant.now();
            CreateDto.User user = new CreateDto.User(
                SimpleSnowflakeId.nextId(), username, name, email,
                passwordEncoder.encode(password), createdAt
            );
            userOrchestrator.saveUser(user);

            // публикуем событие на сохранение токена
            CreateDto.VerificationToken verificationToken = new CreateDto.VerificationToken(
                SimpleSnowflakeId.nextId(), user.getId(), generateBase64String(),
                TokenType.EMAIL_UPDATE, createdAt, 24 // 24 часа
            );
            eventPublisher.publishEvent(new AsyncEvent.SaveVerificationToken(verificationToken));

            // отправляем подтверждение активации аккаунта на почту
            emailNotifier.sendVerificationTokenMail(email, verificationToken.getTokenType(), verificationToken.getToken());

            log.info("[🔧] ✅ User registered successfully --> {}", username);
            return ResultOneArg.success("User registered successfully. Check your mail to activate your account!!!");
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to register user: {}", e.getMessage());
            return ResultOneArg.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Registration failed for user {}: {}", username, e.getMessage());
            return ResultOneArg.error("registerUser failed due to server error");
        }
    }

    @Transactional
    public ResultOneArg<Dto.UserLogin> authenticateUser(@NonNull String username, @NonNull String password, HttpServletRequest httpRequest) {
        try {
            UserSecurity user = userOrchestrator.getActiveUserSecurityByUsername(username)
                    .orElseThrow(() -> new ValidationException("Invalid username or password"));

            if (!passwordEncoder.matches(password, user.hashPassword())) {
                throw new ValidationException("Invalid username or password");
            }

            // публикуем событие на сохранение истории логинов и обновление последнего логина
            CreateDto.LoginHistory loginHistory = new CreateDto.LoginHistory(
                SimpleSnowflakeId.nextId(), user.id(), extractClientIp(httpRequest), 
                httpRequest.getHeader("User-Agent"), Instant.now()
            );
            eventPublisher.publishEvent(new AsyncEvent.SaveUserLoginHistory(username, loginHistory));

            String token = jwtUtil.generateToken(user.id(), user.jwtVersion());

            log.info("[🔧] ✅ User logged in successfully --> {}", username);
            return ResultOneArg.success(new Dto.UserLogin(token, jwtUtil.getTokenExpirationTime(token)));
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to authenticate user: {}", e.getMessage());
            return ResultOneArg.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error on authentication for user {}: {}", username, e.getMessage());
            return ResultOneArg.error("authenticateUser failed due to server error");
        }
    }

    @Transactional
    public ResultNoArgs requestEmailUpdate(long userId) {
        try {
            UserSecurity user = userOrchestrator.getUserSecurity(userId)
                    .orElseThrow(() -> new ValidationException("User not found"));

            // Генерация токена
            String token = generateBase64String();
            TokenType tokenType = TokenType.EMAIL_UPDATE;
            Instant now = Instant.now();
            CreateDto.VerificationToken verificationToken = new CreateDto.VerificationToken(
                SimpleSnowflakeId.nextId(), userId, token, tokenType, now, 24 // 24 часа
            );
            verificationTokenOrchestrator.saveVerificationToken(verificationToken);

            // Отправляем письмо на старый email
            emailNotifier.sendVerificationTokenMail(user.email(), tokenType, token);
            return ResultNoArgs.success();
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to send email update token: {}", e.getMessage());
            return ResultNoArgs.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error on sending token confirmation for email update: {}", e.getMessage());
            return ResultNoArgs.error("requestEmailUpdate failed due to server error");
        }
    }

    @Transactional
    public ResultNoArgs requestPasswordUpdate(@NonNull String email) {
        try {
            UserSecurity user = userOrchestrator.getActiveUserSecurityByEmail(email)
                    .orElseThrow(() -> new ValidationException("User with such email does not exist"));

            // Генерация токена
            String token = generateBase64String();
            TokenType tokenType = TokenType.PASSWORD_UPDATE;
            Instant now = Instant.now();
            CreateDto.VerificationToken verificationToken = new CreateDto.VerificationToken(
                SimpleSnowflakeId.nextId(), user.id(), token, tokenType, now, 1 // 1 час
            );
            verificationTokenOrchestrator.saveVerificationToken(verificationToken);

            // Отправляем письмо на старый email
            emailNotifier.sendVerificationTokenMail(user.email(), tokenType, token);
            return ResultNoArgs.success();
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to send password reset token: {}", e.getMessage());
            return ResultNoArgs.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error on sending token confirmation for password reset: {}", e.getMessage());
            return ResultNoArgs.error("requestPasswordUpdate failed due to server error");
        }
    }

    @Transactional
    public ResultOneArg<String> confirmRegistrationToken(@NonNull String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                throw new ValidationException("Token cannot be empty");
            }

            VerificationToken verificationToken = verificationTokenOrchestrator.getVerificationToken(token)
                    .orElseThrow(() -> new ValidationException("Invalid token"));

            if (verificationToken.expiryDate().isBefore(Instant.now())) {
                throw new ValidationException("Token expired");
            } else if (verificationToken.tokenType() != TokenType.REGISTRATION) {
                throw new ValidationException("Invalid token type");
            }

            long userId = verificationToken.userId();

            // TODO: Надо проверку на то, не удаленный ли аккаунт
            // validator.validateActiveUser(userId);

            Instant updatedAt = Instant.now();

            userOrchestrator.enableUser(userId, updatedAt);

            // публикуем событие на удаление токена
            eventPublisher.publishEvent(new AsyncEvent.DeleteVerificationToken(token));

            log.info("[🔧] ✅ Registration verified successfully for user {}", userId);
            return ResultOneArg.success("Registration successfully verified");
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to confirm registration token: {}", e.getMessage());
            return ResultOneArg.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error on registration token confirmation: {}", e.getMessage());
            return ResultOneArg.error("confirmRegistrationToken failed due to server error");
        }
    }

    @Transactional
    public ResultOneArg<String> confirmEmailUpdateToken(@NonNull String token, @NonNull String email) {
        try {
            if (token == null || token.trim().isEmpty()) {
                throw new ValidationException("Token cannot be empty");
            }

            VerificationToken verificationToken = verificationTokenOrchestrator.getVerificationToken(token)
                    .orElseThrow(() -> new ValidationException("Invalid token"));

            if (verificationToken.expiryDate().isBefore(Instant.now())) {
                throw new ValidationException("Token expired");
            } else if (verificationToken.tokenType() != TokenType.EMAIL_UPDATE) {
                throw new ValidationException("Invalid token type");
            }

            long userId = verificationToken.userId();

            UserSecurity user = userOrchestrator.getActiveUserSecurity(userId)
                    .orElseThrow(() -> new ValidationException("User not found"));

            Instant updatedAt = Instant.now();

            userOrchestrator.updateUserEmail(userId, user.email(), email, updatedAt);

            // публикуем событие на удаление токена
            eventPublisher.publishEvent(new AsyncEvent.DeleteVerificationToken(token));

            log.info("[🔧] ✅ Email changed successfully for user {}", userId);
            return ResultOneArg.success("Email successfully changed");
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to confirm email change token: {}", e.getMessage());
            return ResultOneArg.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error on email change token confirmation: {}", e.getMessage());
            return ResultOneArg.error("confirmEmailUpdateToken failed due to server error");
        }
    }

    @Transactional
    public ResultOneArg<String> confirmPasswordUpdateToken(String token, String password) {
        try {
            if (token == null || token.trim().isEmpty()) {
                throw new ValidationException("Token cannot be empty");
            }

            VerificationToken verificationToken = verificationTokenOrchestrator.getVerificationToken(token)
                    .orElseThrow(() -> new ValidationException("Invalid token"));

            if (verificationToken.expiryDate().isBefore(Instant.now())) {
                throw new ValidationException("Token expired");
            } else if (verificationToken.tokenType() != TokenType.PASSWORD_UPDATE) {
                throw new ValidationException("Invalid token type");
            }

            long userId = verificationToken.userId();

            validator.validateActiveUser(userId);

            Instant updatedAt = Instant.now();

            userOrchestrator.updateUserPassword(userId, password, updatedAt);

            // публикуем событие на удаление токена
            eventPublisher.publishEvent(new AsyncEvent.DeleteVerificationToken(token));

            log.info("[🔧] ✅ Password changed successfully for user {}", userId);
            return ResultOneArg.success("Password successfully changed");
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to confirm password change token: {}", e.getMessage());
            return ResultOneArg.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error on password change token confirmation: {}", e.getMessage());
            return ResultOneArg.error("confirmPasswordUpdateToken failed due to server error");
        }
    }

    public ResultOneArg<String> getActuatorToken(HttpServletRequest httpRequest) {
        try {
            String adminUsername = "monitoring";
            String token = jwtUtil.generateTokenForActuator(adminUsername, 3600L);

            log.info("[🔧] ✅ Actuator token generated for monitoring");
            return ResultOneArg.success(token);
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Failed to generate actuator token: {}", e.getMessage());
            return ResultOneArg.error("Failed to generate token: " + e.getMessage());
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        try {
            if (request.getHeader("X-Forwarded-For") instanceof String xfHeader && !xfHeader.isEmpty()) {
                return xfHeader.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    private static String generateBase64String() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[48]; // 48 bytes = 64 base64 characters
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
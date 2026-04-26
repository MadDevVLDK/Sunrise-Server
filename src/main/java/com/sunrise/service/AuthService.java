package com.sunrise.service;

import com.sunrise.dataservice.DataValidator;
import com.sunrise.dataservice.type.TokenType;
import com.sunrise.notifier.AsyncEvent;
import com.sunrise.service.result.*;
import com.sunrise.service.creation.CreateUserDTO;
import com.sunrise.dataservice.result.UserSecurityDTO;
import com.sunrise.service.creation.CreateLoginHistoryDTO;
import com.sunrise.service.creation.CreateVerificationTokenDTO;
import com.sunrise.dataservice.DataOrchestrator;
import com.sunrise.helpclass.jwt.JwtUtil;
import com.sunrise.dataservice.LockManager;
import com.sunrise.notifier.EmailNotifier;
import com.sunrise.dataservice.result.VerificationTokenDTO;
import com.sunrise.helpclass.SimpleSnowflakeId;
import com.sunrise.helpclass.ValidationException;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.NonNull;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Slf4j
@RequiredArgsConstructor
@Service
public class AuthService {

    private final ApplicationEventPublisher eventPublisher;
    private final EmailNotifier emailNotifier;

    private final DataOrchestrator dataOrchestrator;
    private final DataValidator validator;
    private final LockManager lockManager;

    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;

    public ResultOneArg<String> registerUser(@NonNull String username, @NonNull String name, @NonNull String email, @NonNull String password) {
        try {
            // пытаемся заблокировать регистрацию
            if (!lockManager.tryLockRegistration(username, email)) {
                throw new ValidationException("Try again later");
            }

            if (dataOrchestrator.existsUserByUsername(username.trim())) {
                throw new ValidationException("Username already exists");
            }

            if (dataOrchestrator.existsUserByEmail(email.toLowerCase())) {
                throw new ValidationException("Email already exists");
            }

            LocalDateTime createdAt = LocalDateTime.now();
            CreateUserDTO user = new CreateUserDTO(
                SimpleSnowflakeId.nextId(), username, name, email,
                passwordEncoder.encode(password), createdAt
            );
            dataOrchestrator.saveUser(user);

            // публикуем событие на сохранение токена
            CreateVerificationTokenDTO verificationToken = new CreateVerificationTokenDTO(
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
        finally {
            lockManager.unLockRegistration(username, email); // разблокируем регистрацию
        }
    }
    public ResultOneArg<UserLoginResult> authenticateUser(@NonNull String username, @NonNull String password, HttpServletRequest httpRequest) {
        try {
            UserSecurityDTO user = dataOrchestrator.getActiveUserSecurityByUsername(username)
                    .orElseThrow(() -> new ValidationException("Invalid username or password"));

            if (!passwordEncoder.matches(password, user.getHashPassword())) {
                throw new ValidationException("Invalid username or password");
            }

            // публикуем событие на сохранение истории логинов и обновление последнего логина
            CreateLoginHistoryDTO loginHistory = new CreateLoginHistoryDTO(
                SimpleSnowflakeId.nextId(), user.getId(),
                extractClientIp(httpRequest), httpRequest.getHeader("User-Agent"), LocalDateTime.now()
            );
            eventPublisher.publishEvent(new AsyncEvent.SaveUserLoginHistory(username, loginHistory));

            String token = jwtUtil.generateToken(user.getId(), user.getJwtVersion());

            log.info("[🔧] ✅ User logged in successfully --> {}", username);
            return ResultOneArg.success(new UserLoginResult(token, jwtUtil.getTokenExpirationTime(token)));
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

    public ResultNoArgs requestEmailUpdate(long userId) {
        try {
            UserSecurityDTO user = dataOrchestrator.getUserSecurity(userId)
                    .orElseThrow(() -> new ValidationException("User not found"));

            // Генерация токена
            String token = generateBase64String();
            TokenType tokenType = TokenType.EMAIL_UPDATE;
            LocalDateTime now = LocalDateTime.now();
            CreateVerificationTokenDTO verificationToken = new CreateVerificationTokenDTO(
                SimpleSnowflakeId.nextId(), userId, token, tokenType, now, 24 // 24 часа
            );
            dataOrchestrator.saveVerificationToken(verificationToken);

            // Отправляем письмо на старый email
            emailNotifier.sendVerificationTokenMail(user.getEmail(), tokenType, token);
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
    public ResultNoArgs requestPasswordUpdate(@NonNull String email) {
        try {
            UserSecurityDTO user = dataOrchestrator.getActiveUserSecurityByEmail(email)
                    .orElseThrow(() -> new ValidationException("User with such email does not exist"));

            // Генерация токена
            String token = generateBase64String();
            TokenType tokenType = TokenType.PASSWORD_UPDATE;
            LocalDateTime now = LocalDateTime.now();
            CreateVerificationTokenDTO verificationToken = new CreateVerificationTokenDTO(
                SimpleSnowflakeId.nextId(), user.getId(), token, tokenType, now, 1 // 1 час
            );
            dataOrchestrator.saveVerificationToken(verificationToken);

            // Отправляем письмо на старый email
            emailNotifier.sendVerificationTokenMail(user.getEmail(), tokenType, token);
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

    public ResultOneArg<String> confirmRegistrationToken(@NonNull String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                throw new ValidationException("Token cannot be empty");
            }

            VerificationTokenDTO verificationToken = dataOrchestrator.getVerificationToken(token)
                    .orElseThrow(() -> new ValidationException("Invalid token"));

            if (verificationToken.isExpired()) {
                throw new ValidationException("Token expired");
            } else if (verificationToken.getTokenType() != TokenType.REGISTRATION) {
                throw new ValidationException("Invalid token type");
            }

            long userId = verificationToken.getUserId();

            validator.validateActiveUser(userId);

            LocalDateTime updatedAt = LocalDateTime.now();

            dataOrchestrator.enableUser(userId, updatedAt);

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
    public ResultOneArg<String> confirmEmailUpdateToken(@NonNull String token, @NonNull String email) {
        try {
            if (token == null || token.trim().isEmpty()) {
                throw new ValidationException("Token cannot be empty");
            }

            VerificationTokenDTO verificationToken = dataOrchestrator.getVerificationToken(token)
                    .orElseThrow(() -> new ValidationException("Invalid token"));

            if (verificationToken.isExpired()) {
                throw new ValidationException("Token expired");
            } else if (verificationToken.getTokenType() != TokenType.EMAIL_UPDATE) {
                throw new ValidationException("Invalid token type");
            }

            long userId = verificationToken.getUserId();

            UserSecurityDTO user = dataOrchestrator.getActiveUserSecurity(userId)
                    .orElseThrow(() -> new ValidationException("User not found"));

            LocalDateTime updatedAt = LocalDateTime.now();

            dataOrchestrator.updateUserEmail(userId, user.getEmail(), email, updatedAt);

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
    public ResultOneArg<String> confirmPasswordUpdateToken(String token, String password) {
        try {
            if (token == null || token.trim().isEmpty()) {
                throw new ValidationException("Token cannot be empty");
            }

            VerificationTokenDTO verificationToken = dataOrchestrator.getVerificationToken(token)
                    .orElseThrow(() -> new ValidationException("Invalid token"));

            if (verificationToken.isExpired()) {
                throw new ValidationException("Token expired");
            } else if (verificationToken.getTokenType() != TokenType.PASSWORD_UPDATE) {
                throw new ValidationException("Invalid token type");
            }

            long userId = verificationToken.getUserId();

            validator.validateActiveUser(userId);

            LocalDateTime updatedAt = LocalDateTime.now();

            dataOrchestrator.updateUserPassword(userId, password, updatedAt);

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


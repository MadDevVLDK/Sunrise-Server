package com.sunrise.core.service;

import com.sunrise.core.creation.CreateDto;
import com.sunrise.helpclass.JwtUtil;
import com.sunrise.helpclass.SnowflakeId;
import com.sunrise.helpclass.exception.MyErrorCode;
import com.sunrise.helpclass.exception.MyException;
import com.sunrise.orchestrator.DataValidator;
import com.sunrise.orchestrator.event.AsyncEvent;
import com.sunrise.orchestrator.result.Dto;
import com.sunrise.orchestrator.result.Dto.*;
import com.sunrise.orchestrator.service.UserOrchestrator;
import com.sunrise.orchestrator.service.VerificationTokenOrchestrator;
import com.sunrise.orchestrator.type.TokenType;
import com.sunrise.web.email.EmailNotifier;

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
    public String registerUser(@NonNull String username, @NonNull String name, @NonNull String email, @NonNull String password) {
        if (userOrchestrator.existsUserByUsername(username.trim())) {
            throw new MyException(
                MyErrorCode.USERNAME_TAKEN, 
                "Username already taken -> " + username
            );
        }

        if (userOrchestrator.existsUserByEmail(email.toLowerCase())) {
            throw new MyException(
                MyErrorCode.EMAIL_TAKEN, 
                "Email already taken -> " + email
            );
        }

        Instant createdAt = Instant.now();
        CreateDto.User user = new CreateDto.User(
            SnowflakeId.next(), username, name, email,
            passwordEncoder.encode(password), createdAt
        );
        userOrchestrator.saveUser(user);

        CreateDto.VerificationToken verificationToken = new CreateDto.VerificationToken(
            SnowflakeId.next(), user.getId(), generateBase64String(),
            TokenType.REGISTRATION, createdAt, 24
        );
        eventPublisher.publishEvent(new AsyncEvent.SaveVerificationToken(verificationToken));

        emailNotifier.sendVerificationTokenMail(email, verificationToken.getTokenType(), verificationToken.getToken());

        log.info("[🔧] ✅ User registered successfully --> {}", username);
        return "User registered successfully. Check your mail to activate your account!!!";
    }

    @Transactional
    public Dto.UserLogin authenticateUser(@NonNull String username, @NonNull String password, HttpServletRequest httpRequest) {
        UserSecurity user = userOrchestrator.getActiveUserSecurityByUsername(username)
            .orElseThrow(() -> new MyException(
                MyErrorCode.INVALID_CREDENTIALS, 
                "Invalid username or password -> " + username
            ));

        if (!passwordEncoder.matches(password, user.hashPassword())) {
            throw new MyException(
                MyErrorCode.INVALID_CREDENTIALS, 
                "Invalid username or password -> " + username
            );
        }

        CreateDto.LoginHistory loginHistory = new CreateDto.LoginHistory(
            SnowflakeId.next(), user.id(), extractClientIp(httpRequest),
            httpRequest.getHeader("User-Agent"), Instant.now()
        );
        eventPublisher.publishEvent(new AsyncEvent.SaveUserLoginHistory(username, loginHistory));

        String token = jwtUtil.generateToken(user.id(), user.jwtVersion());

        log.info("[🔧] ✅ User logged in successfully --> {}", username);
        return new Dto.UserLogin(token, jwtUtil.getTokenExpirationTime(token));
    }

    @Transactional
    public void requestEmailUpdate(long userId) {
        UserSecurity user = userOrchestrator.getUserSecurity(userId)
            .orElseThrow(() -> new MyException(
                MyErrorCode.USER_NOT_FOUND, 
                "User not found -> " + userId
            ));

        String token = generateBase64String();
        TokenType tokenType = TokenType.EMAIL_UPDATE;
        Instant now = Instant.now();
        CreateDto.VerificationToken verificationToken = new CreateDto.VerificationToken(
            SnowflakeId.next(), userId, token, tokenType, now, 24
        );
        verificationTokenOrchestrator.saveVerificationToken(verificationToken);

        emailNotifier.sendVerificationTokenMail(user.email(), tokenType, token);
        log.info("[🔧] ✅ Email update token sent to user {}", userId);
    }

    @Transactional
    public void requestPasswordUpdate(@NonNull String email) {
        UserSecurity user = userOrchestrator.getActiveUserSecurityByEmail(email)
            .orElseThrow(() -> new MyException(
                MyErrorCode.USER_NOT_FOUND, 
                "User with such email does not exist -> " + email
            ));

        String token = generateBase64String();
        TokenType tokenType = TokenType.PASSWORD_UPDATE;
        Instant now = Instant.now();
        CreateDto.VerificationToken verificationToken = new CreateDto.VerificationToken(
            SnowflakeId.next(), user.id(), token, tokenType, now, 1
        );
        verificationTokenOrchestrator.saveVerificationToken(verificationToken);

        emailNotifier.sendVerificationTokenMail(user.email(), tokenType, token);
        log.info("[🔧] ✅ Password reset token sent to {}", email);
    }

    @Transactional
    public String confirmRegistrationToken(@NonNull String token) {
        VerificationToken verificationToken = getAndValidateToken(token, TokenType.REGISTRATION);

        long userId = verificationToken.userId();
        Instant updatedAt = Instant.now();

        userOrchestrator.enableUser(userId, updatedAt);
        eventPublisher.publishEvent(new AsyncEvent.DeleteVerificationToken(token));

        log.info("[🔧] ✅ Registration verified successfully for user {}", userId);
        return "Registration successfully verified";
    }

    @Transactional
    public String confirmEmailUpdateToken(@NonNull String token, @NonNull String email) {
        VerificationToken verificationToken = getAndValidateToken(token, TokenType.EMAIL_UPDATE);

        long userId = verificationToken.userId();

        UserSecurity user = userOrchestrator.getActiveUserSecurity(userId)
            .orElseThrow(() -> new MyException(
                MyErrorCode.USER_NOT_ACTIVE, 
                "User is not active for email update -> " + userId
            ));

        Instant updatedAt = Instant.now();
        userOrchestrator.updateUserEmail(userId, user.email(), email, updatedAt);

        eventPublisher.publishEvent(new AsyncEvent.DeleteVerificationToken(token));

        log.info("[🔧] ✅ Email changed successfully for user {}", userId);
        return "Email successfully changed";
    }

    @Transactional
    public String confirmPasswordUpdateToken(@NonNull String token, @NonNull String password) {
        VerificationToken verificationToken = getAndValidateToken(token, TokenType.PASSWORD_UPDATE);

        long userId = verificationToken.userId();
        validator.validateActiveUser(userId);

        Instant updatedAt = Instant.now();
        userOrchestrator.updateUserPassword(userId, password, updatedAt);

        eventPublisher.publishEvent(new AsyncEvent.DeleteVerificationToken(token));

        log.info("[🔧] ✅ Password changed successfully for user {}", userId);
        return "Password successfully changed";
    }

    public String getActuatorToken(HttpServletRequest httpRequest) {
        String adminUsername = "monitoring";
        String token = jwtUtil.generateTokenForActuator(adminUsername, 3600L);
        log.info("[🔧] ✅ Actuator token generated for monitoring");
        return token;
    }

    // ===== PRIVATE =====

    private VerificationToken getAndValidateToken(@NonNull String token, TokenType expectedType) {
        if (token.trim().isEmpty()) {
            throw new MyException(MyErrorCode.VALIDATION_ERROR, "Token cannot be empty");
        }

        VerificationToken verificationToken = verificationTokenOrchestrator.getVerificationToken(token)
            .orElseThrow(() -> new MyException(
                MyErrorCode.VERIFICATION_TOKEN_NOT_FOUND,
                "Verification token not found for type " + expectedType
            ));

        if (verificationToken.expiryDate().isBefore(Instant.now())) {
            throw new MyException(
                MyErrorCode.VERIFICATION_TOKEN_EXPIRED, 
                "Verification token expired for user " + verificationToken.userId()
            );
        }

        if (verificationToken.tokenType() != expectedType) {
            throw new MyException(
                MyErrorCode.VERIFICATION_TOKEN_TYPE_MISMATCH,
                "Expected token type " + expectedType + " but got " + verificationToken.tokenType()
            );
        }

        return verificationToken;
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
        byte[] bytes = new byte[48];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
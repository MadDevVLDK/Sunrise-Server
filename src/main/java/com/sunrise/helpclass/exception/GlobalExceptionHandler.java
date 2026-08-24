package com.sunrise.helpclass.exception;

import com.sunrise.web.payload.ApiErrorBody;
import com.sunrise.web.payload.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Map<MyErrorCode, HttpStatus> CODE_TO_STATUS = Map.ofEntries(
        Map.entry(MyErrorCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED),
        Map.entry(MyErrorCode.TOKEN_EXPIRED, HttpStatus.UNAUTHORIZED),
        Map.entry(MyErrorCode.TOKEN_INVALID, HttpStatus.UNAUTHORIZED),
        Map.entry(MyErrorCode.TOKEN_VERSION_MISMATCH, HttpStatus.UNAUTHORIZED),
        Map.entry(MyErrorCode.ACCESS_DENIED, HttpStatus.FORBIDDEN),
        Map.entry(MyErrorCode.MEMBER_NOT_ADMIN, HttpStatus.FORBIDDEN),
        Map.entry(MyErrorCode.MESSAGE_NOT_SENDER, HttpStatus.FORBIDDEN),
        Map.entry(MyErrorCode.USER_NOT_FOUND, HttpStatus.NOT_FOUND),
        Map.entry(MyErrorCode.CHAT_NOT_FOUND, HttpStatus.NOT_FOUND),
        Map.entry(MyErrorCode.MEMBER_NOT_FOUND, HttpStatus.NOT_FOUND),
        Map.entry(MyErrorCode.MESSAGE_NOT_FOUND, HttpStatus.NOT_FOUND),
        Map.entry(MyErrorCode.VERIFICATION_TOKEN_NOT_FOUND, HttpStatus.NOT_FOUND),
        Map.entry(MyErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND),
        Map.entry(MyErrorCode.USER_ALREADY_EXISTS, HttpStatus.CONFLICT),
        Map.entry(MyErrorCode.USERNAME_TAKEN, HttpStatus.CONFLICT),
        Map.entry(MyErrorCode.EMAIL_TAKEN, HttpStatus.CONFLICT),
        Map.entry(MyErrorCode.MEMBER_ALREADY_EXISTS, HttpStatus.CONFLICT),
        Map.entry(MyErrorCode.CONFLICT, HttpStatus.CONFLICT),
        Map.entry(MyErrorCode.CHAT_DELETED, HttpStatus.GONE),
        Map.entry(MyErrorCode.MESSAGE_DELETED, HttpStatus.GONE),
        Map.entry(MyErrorCode.VERIFICATION_TOKEN_EXPIRED, HttpStatus.GONE),
        Map.entry(MyErrorCode.RATE_LIMITED, HttpStatus.TOO_MANY_REQUESTS),
        Map.entry(MyErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR)
    );

    @ExceptionHandler(MyException.class)
    public ResponseEntity<?> handleAppException(MyException ex) {
        HttpStatus status = CODE_TO_STATUS.getOrDefault(ex.getCode(), HttpStatus.BAD_REQUEST);
        ApiErrorBody body = new ApiErrorBody(ex.getCode().name(), ex.getMessage());

        if (status.is5xxServerError()) {
            log.error("[❌] AppException: code={}, message={}", ex.getCode(), ex.getMessage(), ex);
        } else {
            log.warn("[⚠️] AppException: code={}, message={}", ex.getCode(), ex.getMessage());
        }

        return ApiResponse.error(body, status);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        log.warn("[⚠️] Validation failed: {}", message);
        ApiErrorBody body = new ApiErrorBody("VALIDATION_ERROR", message);
        return ApiResponse.error(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleNotReadableHttpMessage(HttpMessageNotReadableException ex) {
        log.warn("[⚠️] Not readable HTTP message: {}", ex.getMessage());
        ApiErrorBody body = new ApiErrorBody("INVALID_INPUT", "Malformed request body");
        return ApiResponse.error(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericException(Exception ex) {
        log.error("[❌] Unhandled exception: {}", ex.getMessage(), ex);
        ApiErrorBody body = new ApiErrorBody("INTERNAL_ERROR", "Internal server error");
        return ApiResponse.error(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
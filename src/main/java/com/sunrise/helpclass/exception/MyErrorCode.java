package com.sunrise.helpclass.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MyErrorCode {

    // ==================== AUTH / SECURITY ====================
    UNAUTHORIZED("Authentication required"),
    TOKEN_EXPIRED("Token has expired"),
    TOKEN_INVALID("Invalid token"),
    TOKEN_VERSION_MISMATCH("Token version mismatch"),
    ACCESS_DENIED("Access denied"),

    // ==================== VALIDATION ====================
    VALIDATION_ERROR("Validation failed"),
    INVALID_INPUT("Invalid input data"),

    // ==================== USER ====================
    USER_NOT_FOUND("User not found"),
    USER_NOT_ACTIVE("User is not active"),
    USER_NOT_FOUND_OR_DELETED("User not found or is deleted"),
    USER_ALREADY_EXISTS("User already exists"),
    USERNAME_TAKEN("Username already taken"),
    EMAIL_TAKEN("Email already exists"),
    INVALID_CREDENTIALS("Invalid username or password"),

    // ==================== CHAT ====================
    CHAT_NOT_FOUND("Chat not found"),
    CHAT_DELETED("Chat is deleted"),
    CHAT_NOT_FOUND_OR_DELETED("Chat not found or is deleted"),
    CHAT_NOT_GROUP("Chat is not a group chat"),
    CHAT_INFO_NOT_CHANGEABLE("Chat info is not changeable"),
    CHAT_MEMBERS_LIMIT("Members count out of bounds"),

    // ==================== CHAT MEMBER ====================
    MEMBER_NOT_FOUND("User is not a member of this chat"),
    MEMBER_NOT_FOUND_OR_DELETED("User is not a member of this chat or is deleted"),
    MEMBER_ALREADY_EXISTS("User is already a member of this chat"),
    MEMBER_NOT_ADMIN("User is not a group admin"),

    // ==================== MESSAGE ====================
    MESSAGE_NOT_FOUND("Message not found"),
    MESSAGE_DELETED("Message is deleted"),
    MESSAGE_NOT_FOUND_OR_DELETED("Message not found or is deleted"),
    MESSAGE_EMPTY("Message text cannot be empty"),
    MESSAGE_TOO_LONG("Message text is too long"),
    MESSAGE_NOT_SENDER("User is not the message sender"),

    // ==================== TOKEN / VERIFICATION ====================
    VERIFICATION_TOKEN_NOT_FOUND("Invalid verification token"),
    VERIFICATION_TOKEN_EXPIRED("Verification token has expired"),
    VERIFICATION_TOKEN_TYPE_MISMATCH("Invalid token type"),

    // ==================== GENERAL ====================
    NOT_FOUND("Resource not found"),
    CONFLICT("Conflict"),
    RATE_LIMITED("Too many requests"),
    INTERNAL_ERROR("Internal server error");

    private final String defaultMessage;
}
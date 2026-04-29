package com.sunrise.web.payload;

import com.sunrise.web.api.annotation.ValidId;
import com.sunrise.orchestrator.type.Direction;

import jakarta.validation.constraints.*;

import java.util.Map;
import java.util.Set;

public final class ApiRequest {

    public record Login(
        @NotBlank(message = "Username is required")
        @Size(min = 4, max = 30, message = "Username must be between 4 and 30 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username must contain only letters, digits, and underscores")
        String username,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 50, message = "Password must be between 8 and 50 characters")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$", message = "Password must contain at least one lowercase letter, one uppercase letter, one digit")
        String password
    ) {}

    public record Register(
        @NotBlank(message = "Username is required")
        @Size(min = 4, max = 30, message = "Username must be between 4 and 30 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username must contain only letters, digits, and underscores")
        String username,

        @NotBlank(message = "Name is required")
        @Size(min = 4, max = 30, message = "Name must be between 4 and 30 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Name must contain only letters, digits, and underscores")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 50, message = "Password must be between 8 and 50 characters")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$", message = "Password must contain at least one lowercase letter, one uppercase letter, one digit")
        String password
    ) {}

    public record ProfileUpdate(
        @NotBlank(message = "Username is required")
        @Size(min = 4, max = 30, message = "Username must be between 4 and 30 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username must contain only letters, digits, and underscores")
        String username,

        @NotBlank(message = "Name is required")
        @Size(min = 4, max = 30, message = "Name must be between 4 and 30 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Name must contain only letters, digits, and underscores")
        String name
    ) {}


    public record AddGroupMember(
        @NotNull(message = "newUserId cannot be null")
        @Min(value = 1, message = "newUserId must be at least 1")
        @Max(value = Long.MAX_VALUE, message = "newUserId must be at most " + Long.MAX_VALUE)
        Long newUserId
    ) {}

    public record AddGroupMembers(
        @NotNull(message = "members is required")
        @NotEmpty(message = "members cannot be empty")
        @Size(max = 100, message = "Cannot add more than 100 members in one request")
        Set<@ValidId Long> members
    ) {}

    public record CreateGroupChat(
        @ValidId Long tempId,
        @NotBlank(message = "chatName is required")
        @Size(min = 4, max = 30, message = "chatName must be between 4 and 30 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "chatName must contain only letters, digits, and underscores")
        String chatName,
        @Size(max = 500, message = "chatDescription mustn`t be more than 500 characters")
        String chatDescription,
        @NotNull(message = "members is required")
        @Size(max = 100, message = "Group cannot have more than 100 members")
        Set<@ValidId Long> members
    ) {}

    public record CreatePersonalChat(
        @ValidId 
        Long tempId,

        @ValidId 
        Long otherUserId
    ) {}

    public record UpdateChatInfo(
        @NotBlank(message = "chatName is required")
        @Size(min = 4, max = 30, message = "chatName must be between 4 and 30 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "chatName must contain only letters, digits, and underscores")
        String chatName,

        @Size(max = 500, message = "chatDescription mustn`t be more than 500 characters")
        String chatDescription
    ) {}
    

    public record UpdateChatMemberInfo(
        @Size(max = 20, message = "tag mustn`t be more than 20 characters") 
        String tag
    ) {}
    
    public record UpdateAdminRights(
        @NotNull(message = "isAdmin is required") 
        Boolean isAdmin
    ) {}
    
    public record UpdateSelfChatSettings(
        @NotNull(message = "isPinned is required") 
        Boolean isPinned
    ) {}


    public record PrivateMessage(
        @ValidId 
        Long tempId,

        @NotBlank 
        @Size(max = 10000) 
        String text,

        @ValidId 
        Long receiverId
    ) {}

    public record PublicMessage(
        @ValidId 
        Long tempId,

        @NotBlank 
        @Size(max = 10000) 
        String text
    ) {}

    public record UpdateMessage(
        @NotBlank 
        @Size(max = 10000) 
        String text
    ) {}


    public record Batch(
        @NotEmpty 
        Set<@ValidId Long> ids
    ) { }

    public record UserPagination(
        String filter,
        
        @Positive 
        Long cursor,

        @Min(10) 
        @Max(100) 
        Integer limit) {

        public Integer getLimit() {
            return limit != null ? limit : 20;
        }
        public String getFilter() {
            return filter != null ? filter : "";
        }
    }

    public record ChatPagination(
        Boolean isPinnedCursor,

        @Positive 
        Long lastMsgIdCursor,

        @Positive 
        Long chatIdCursor,

        @Min(10) 
        @Max(100) 
        Integer limit) {
        public Integer getLimit() {
            return limit != null ? limit : 20;
        }
    }

    public record ChatMemberPagination(
        @Positive 
        Long cursor,

        @Min(10) 
        @Max(100) 
        Integer limit) {

        public Integer getLimit() {
            return limit != null ? limit : 20;
        }
    }

    public record MessagePagination(
        @NotNull 
        Direction direction,
        
        @Positive 
        Long cursor,

        @Min(10) 
        @Max(100) 
        Integer limit) {

        public Integer getLimit() {
            return limit != null ? limit : 20;
        }
    }

    public record SyncRequest(Map<Long, Long> chatSeqs) {}
}
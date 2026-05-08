package com.sunrise.web.payload;

public final class WsResponse {
    public record UserStatus(long userId, String newStatus) {}
    public record UserChatAction(long chatId, long userId, String action) {}
    public record Pong() {}
    public record Error(String message, String path) {}
}
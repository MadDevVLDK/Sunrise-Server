package com.sunrise.entity.pagination;

import com.sunrise.entity.dto.ChatUserDTO;

import java.util.Map;

public record UserChatsPageDTO(Map<Long, ChatUserDTO> chats, Long nextCursor) { }

package com.sunrise.entity.pagination;

import com.sunrise.entity.dto.ChatProfileDTO;

import java.util.Map;

public record UserChatsPageDTO(Map<Long, ChatProfileDTO> chats, Long nextCursor) { }

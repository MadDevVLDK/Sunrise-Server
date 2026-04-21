package com.sunrise.entity.pagination;

import com.sunrise.entity.dto.UserMessageDTO;

import java.util.Map;

public record MessagesPageDTO (Map<Long, UserMessageDTO> messages, Long nextCursor) {}
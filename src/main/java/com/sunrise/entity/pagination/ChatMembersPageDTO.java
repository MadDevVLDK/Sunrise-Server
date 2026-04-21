package com.sunrise.entity.pagination;

import com.sunrise.entity.dto.ChatMemberProfileFullDTO;

import java.util.Map;

public record ChatMembersPageDTO(Map<Long, ChatMemberProfileFullDTO> chatMembers, Long nextCursor) { }
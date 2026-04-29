package com.sunrise.orchestrator.result;

import java.util.List;

public record ChatMembersPageDTO(List<ChatMemberProfileFullDTO> chatMembers, Long nextCursor) { }
package com.sunrise.entity.pagination;

import com.sunrise.entity.dto.UserProfileLightDTO;

import java.util.Map;

public record UsersPageDTO(Map<Long, UserProfileLightDTO> users, Long nextCursor) { }
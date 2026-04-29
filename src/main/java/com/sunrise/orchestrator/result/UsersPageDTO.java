package com.sunrise.orchestrator.result;

import java.util.List;

public record UsersPageDTO(List<UserProfileLightDTO> users, Long nextCursor) { }
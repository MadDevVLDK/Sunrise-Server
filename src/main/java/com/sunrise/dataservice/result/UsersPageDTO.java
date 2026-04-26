package com.sunrise.dataservice.result;

import java.util.List;

public record UsersPageDTO(List<UserProfileLightDTO> users, Long nextCursor) { }
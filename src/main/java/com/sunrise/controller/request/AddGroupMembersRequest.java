package com.sunrise.controller.request;

import com.sunrise.config.annotation.ValidId;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

@lombok.Getter
@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
public class AddGroupMembersRequest {
    @NotNull(message = "members is required")
    @NotEmpty(message = "members cannot be empty")
    @Size(max = 100, message = "Cannot add more than 100 members in one request")
    private Set<@ValidId Long> members;
}

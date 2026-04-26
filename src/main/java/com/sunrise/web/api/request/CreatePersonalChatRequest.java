package com.sunrise.web.api.request;

import com.sunrise.web.api.annotation.ValidId;

@lombok.Getter
@lombok.Setter
@lombok.AllArgsConstructor
public class CreatePersonalChatRequest {

    @ValidId
    private Long tempId;

    @ValidId
    private Long otherUserId;
}
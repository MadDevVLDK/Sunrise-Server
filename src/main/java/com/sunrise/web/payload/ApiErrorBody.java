package com.sunrise.web.payload;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiErrorBody {
    private final String code;
    private final String message;
}
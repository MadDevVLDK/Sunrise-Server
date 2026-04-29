package com.sunrise.core.result;

public record UserLoginResult(String jwtToken, java.util.Date expiration) { }
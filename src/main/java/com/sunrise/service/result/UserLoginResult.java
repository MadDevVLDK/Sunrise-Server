package com.sunrise.service.result;

public record UserLoginResult(String jwtToken, java.util.Date expiration) { }
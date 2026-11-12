package com.shopsecure.auth.web.dto;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {}

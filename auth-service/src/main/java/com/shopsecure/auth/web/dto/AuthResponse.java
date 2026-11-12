package com.shopsecure.auth.web.dto;

/**
 * The access token goes in the JSON body (frontend keeps it in memory only).
 * The refresh token is NOT in the body: it is set as an HttpOnly cookie that JavaScript can't read,
 * so an XSS bug can't steal the long-lived credential.
 */
public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {}

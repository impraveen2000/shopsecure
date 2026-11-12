package com.shopsecure.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Map;

/** Type-safe binding of the "app.*" section of application.yml. */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Security security, Map<String, String> serviceClients, Admin admin) {

    public record Jwt(String issuer, Duration accessTokenTtl, Duration refreshTokenTtl, Duration serviceTokenTtl) {}

    public record Security(int maxFailedAttempts, Duration lockDuration, boolean cookieSecure) {}

    public record Admin(String username, String email, String password) {}
}

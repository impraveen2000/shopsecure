package com.shopsecure.auth.web;

import com.shopsecure.auth.config.AppProperties;
import com.shopsecure.auth.exception.InvalidTokenException;
import com.shopsecure.auth.service.AuthService;
import com.shopsecure.auth.service.UserService;
import com.shopsecure.auth.web.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    static final String REFRESH_COOKIE = "refresh_token";

    private final AuthService authService;
    private final UserService userService;
    private final AppProperties props;

    public AuthController(AuthService authService, UserService userService, AppProperties props) {
        this.authService = authService;
        this.userService = userService;
        this.props = props;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return tokenResponse(authService.login(request));
    }

    /** Called by the frontend when the access token expires (or on page reload). */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidTokenException("No refresh token");
        }
        return tokenResponse(authService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookie("", Duration.ZERO).toString())
                .build();
    }

    /** Protected: requires a valid Bearer JWT. @AuthenticationPrincipal injects the decoded token. */
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return userService.getByUsername(jwt.getSubject());
    }

    /** Internal machine-to-machine endpoint. The API gateway blocks it from the outside world. */
    @PostMapping("/service-token")
    public TokenResponse serviceToken(@Valid @RequestBody ServiceTokenRequest request) {
        String token = authService.serviceToken(request);
        return new TokenResponse(token, "Bearer", props.jwt().serviceTokenTtl().toSeconds());
    }

    private ResponseEntity<AuthResponse> tokenResponse(AuthService.TokenPair pair) {
        AuthResponse body = new AuthResponse(pair.accessToken(), "Bearer",
                props.jwt().accessTokenTtl().toSeconds(), UserResponse.from(pair.user()));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(pair.refreshToken(), props.jwt().refreshTokenTtl()).toString())
                .body(body);
    }

    /**
     * HttpOnly  -> JavaScript cannot read it (XSS protection)
     * Secure    -> only sent over HTTPS (enable in production)
     * SameSite  -> Strict: never sent on cross-site requests (CSRF protection)
     * Path      -> only sent to /api/auth/*, not to every API call
     */
    private ResponseCookie refreshCookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(props.security().cookieSecure())
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(maxAge)
                .build();
    }
}

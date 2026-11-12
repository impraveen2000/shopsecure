package com.shopsecure.auth.service;

import com.shopsecure.auth.config.AppProperties;
import com.shopsecure.auth.domain.Role;
import com.shopsecure.auth.domain.User;
import com.shopsecure.auth.event.UserEventPublisher;
import com.shopsecure.auth.exception.ConflictException;
import com.shopsecure.auth.exception.InvalidTokenException;
import com.shopsecure.auth.repository.UserRepository;
import com.shopsecure.auth.web.dto.LoginRequest;
import com.shopsecure.auth.web.dto.RegisterRequest;
import com.shopsecure.auth.web.dto.ServiceTokenRequest;
import com.shopsecure.auth.web.dto.UserResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Set;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final RefreshTokenService refreshTokens;
    private final UserEventPublisher events;
    private final AppProperties props;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager,
                       TokenService tokenService, RefreshTokenService refreshTokens, UserEventPublisher events,
                       AppProperties props) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.refreshTokens = refreshTokens;
        this.events = events;
        this.props = props;
    }

    public record TokenPair(String accessToken, String refreshToken, User user) {}

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (users.existsByUsername(request.username())) throw new ConflictException("Username already taken");
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmail(email)) throw new ConflictException("Email already registered");

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRoles(Set.of(Role.USER));       // never trust a role sent by the client
        users.save(user);

        events.userRegistered(user);
        return UserResponse.from(user);
    }

    /** noRollbackFor: we WANT the failed-attempt counter to be saved even though we throw. */
    @Transactional(noRollbackFor = AuthenticationException.class)
    public TokenPair login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (BadCredentialsException e) {
            users.findByUsername(request.username()).ifPresent(this::recordFailedAttempt);
            throw e;
        }

        User user = users.findByUsername(request.username()).orElseThrow();
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        log.info("User '{}' logged in", user.getUsername());
        return new TokenPair(tokenService.createAccessToken(user), refreshTokens.issue(user), user);
    }

    private void recordFailedAttempt(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= props.security().maxFailedAttempts()) {
            user.setLockedUntil(Instant.now().plus(props.security().lockDuration()));
            user.setFailedLoginAttempts(0);
            log.warn("SECURITY: account '{}' locked after {} failed logins", user.getUsername(), attempts);
        }
    }

    @Transactional(noRollbackFor = InvalidTokenException.class)
    public TokenPair refresh(String rawRefreshToken) {
        RefreshTokenService.Rotation rotation = refreshTokens.rotate(rawRefreshToken);
        User user = rotation.user();
        if (!user.isEnabled() || user.isLocked()) {
            refreshTokens.revokeAll(user);
            throw new InvalidTokenException("Account is disabled or locked");
        }
        return new TokenPair(tokenService.createAccessToken(user), rotation.newRawToken(), user);
    }

    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            refreshTokens.revoke(rawRefreshToken);
        }
    }

    /** Client-credentials style token for internal services (e.g. order-service -> product-service). */
    public String serviceToken(ServiceTokenRequest request) {
        String expected = props.serviceClients().get(request.clientId());
        // MessageDigest.isEqual = constant-time comparison (prevents timing attacks)
        if (expected == null || !MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                request.clientSecret().getBytes(StandardCharsets.UTF_8))) {
            log.warn("SECURITY: invalid service credentials for client '{}'", request.clientId());
            throw new BadCredentialsException("Invalid client credentials");
        }
        return tokenService.createServiceToken(request.clientId());
    }
}

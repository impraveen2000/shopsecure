package com.shopsecure.auth.service;

import com.shopsecure.auth.config.AppProperties;
import com.shopsecure.auth.domain.RefreshToken;
import com.shopsecure.auth.domain.User;
import com.shopsecure.auth.exception.InvalidTokenException;
import com.shopsecure.auth.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/**
 * REFRESH TOKEN ROTATION + REUSE DETECTION
 * <p>
 * Every time a refresh token is used, it is revoked and a brand new one is issued.
 * If an already-revoked token is presented again, someone has a copy of it (theft!), so we
 * revoke ALL of that user's sessions. The legitimate user just logs in again; the thief is out.
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    /** Two browser tabs refreshing at the same moment is a race, not theft. */
    private static final Duration REUSE_GRACE = Duration.ofSeconds(10);

    private final SecureRandom random = new SecureRandom();
    private final RefreshTokenRepository repository;
    private final AppProperties props;

    public RefreshTokenService(RefreshTokenRepository repository, AppProperties props) {
        this.repository = repository;
        this.props = props;
    }

    public record Rotation(User user, String newRawToken) {}

    @Transactional
    public String issue(User user) {
        byte[] bytes = new byte[64];
        random.nextBytes(bytes);                       // cryptographically secure randomness
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RefreshToken token = new RefreshToken();
        token.setTokenHash(hash(raw));
        token.setUser(user);
        token.setExpiresAt(Instant.now().plus(props.jwt().refreshTokenTtl()));
        repository.save(token);
        return raw;                                    // the raw value only ever lives in the cookie
    }

    @Transactional(noRollbackFor = InvalidTokenException.class)
    public Rotation rotate(String raw) {
        RefreshToken token = repository.findByTokenHash(hash(raw))
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        if (token.isRevoked()) {
            boolean recentRace = token.getRevokedAt() != null
                    && token.getRevokedAt().isAfter(Instant.now().minus(REUSE_GRACE));
            if (!recentRace) {
                repository.revokeAllForUser(token.getUser(), Instant.now());
                log.warn("SECURITY: refresh token reuse detected for user '{}'. All sessions revoked.",
                        token.getUser().getUsername());
                throw new InvalidTokenException("Refresh token reuse detected. Please log in again.");
            }
            throw new InvalidTokenException("Refresh token already used");
        }
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException("Refresh token expired");
        }

        token.revoke();
        User user = token.getUser();
        return new Rotation(user, issue(user));
    }

    @Transactional
    public void revoke(String raw) {
        repository.findByTokenHash(hash(raw)).ifPresent(RefreshToken::revoke);
    }

    @Transactional
    public void revokeAll(User user) {
        repository.revokeAllForUser(user, Instant.now());
    }

    static String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}

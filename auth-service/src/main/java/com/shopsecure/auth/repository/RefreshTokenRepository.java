package com.shopsecure.auth.repository;

import com.shopsecure.auth.domain.RefreshToken;
import com.shopsecure.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** "Log out everywhere": revoke every active refresh token of a user. */
    @Modifying
    @Query("update RefreshToken t set t.revoked = true, t.revokedAt = :now where t.user = :user and t.revoked = false")
    int revokeAllForUser(@Param("user") User user, @Param("now") Instant now);
}

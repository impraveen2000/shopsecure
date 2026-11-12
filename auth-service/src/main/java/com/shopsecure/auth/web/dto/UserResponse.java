package com.shopsecure.auth.web.dto;

import com.shopsecure.auth.domain.User;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

/** Never return the entity directly: it would leak the password hash. */
public record UserResponse(Long id, String username, String email, Set<String> roles,
                           boolean enabled, boolean locked, Instant createdAt) {

    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getUsername(), u.getEmail(),
                u.getRoles().stream().map(Enum::name).collect(Collectors.toCollection(java.util.TreeSet::new)),
                u.isEnabled(), u.isLocked(), u.getCreatedAt());
    }
}

package com.shopsecure.auth.service;

import com.shopsecure.auth.domain.Role;
import com.shopsecure.auth.domain.User;
import com.shopsecure.auth.exception.NotFoundException;
import com.shopsecure.auth.repository.UserRepository;
import com.shopsecure.auth.web.dto.UserResponse;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class UserService {

    private final UserRepository users;
    private final RefreshTokenService refreshTokens;

    public UserService(UserRepository users, RefreshTokenService refreshTokens) {
        this.users = users;
        this.refreshTokens = refreshTokens;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return users.findAll(Sort.by("id")).stream().map(UserResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getByUsername(String username) {
        return users.findByUsername(username).map(UserResponse::from)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    @Transactional
    public UserResponse updateRoles(Long id, Set<Role> roles, String actingAdmin) {
        User user = load(id);
        if (user.getUsername().equals(actingAdmin) && !roles.contains(Role.ADMIN)) {
            throw new IllegalArgumentException("You cannot remove your own ADMIN role");
        }
        if (roles.isEmpty()) throw new IllegalArgumentException("A user needs at least one role");
        user.setRoles(roles);
        // Existing access tokens still carry old roles until they expire (max 15 min).
        // Revoking refresh tokens forces a fresh login so new roles apply soon.
        refreshTokens.revokeAll(user);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse setEnabled(Long id, boolean enabled, String actingAdmin) {
        User user = load(id);
        if (user.getUsername().equals(actingAdmin) && !enabled) {
            throw new IllegalArgumentException("You cannot disable your own account");
        }
        user.setEnabled(enabled);
        if (!enabled) refreshTokens.revokeAll(user);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse unlock(Long id) {
        User user = load(id);
        user.setLockedUntil(null);
        user.setFailedLoginAttempts(0);
        return UserResponse.from(user);
    }

    private User load(Long id) {
        return users.findById(id).orElseThrow(() -> new NotFoundException("User " + id + " not found"));
    }
}

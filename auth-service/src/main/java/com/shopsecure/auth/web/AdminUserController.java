package com.shopsecure.auth.web;

import com.shopsecure.auth.service.UserService;
import com.shopsecure.auth.web.dto.UpdateRolesRequest;
import com.shopsecure.auth.web.dto.UpdateStatusRequest;
import com.shopsecure.auth.web.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Defense in depth: the URL rule in SecurityConfig AND @PreAuthorize both require ADMIN.
 * If someone later changes the URL mapping by mistake, the method is still protected.
 */
@RestController
@RequestMapping("/api/auth/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserResponse> list() {
        return userService.findAll();
    }

    @PutMapping("/{id}/roles")
    public UserResponse updateRoles(@PathVariable Long id, @Valid @RequestBody UpdateRolesRequest request,
                                    @AuthenticationPrincipal Jwt admin) {
        return userService.updateRoles(id, request.roles(), admin.getSubject());
    }

    @PutMapping("/{id}/status")
    public UserResponse updateStatus(@PathVariable Long id, @RequestBody UpdateStatusRequest request,
                                     @AuthenticationPrincipal Jwt admin) {
        return userService.setEnabled(id, request.enabled(), admin.getSubject());
    }

    @PutMapping("/{id}/unlock")
    public UserResponse unlock(@PathVariable Long id) {
        return userService.unlock(id);
    }
}

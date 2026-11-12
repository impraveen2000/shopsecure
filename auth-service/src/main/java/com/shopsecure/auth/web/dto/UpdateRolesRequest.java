package com.shopsecure.auth.web.dto;

import com.shopsecure.auth.domain.Role;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record UpdateRolesRequest(@NotEmpty Set<Role> roles) {}

package com.shopsecure.auth.web;

import com.shopsecure.auth.config.AppProperties;
import com.shopsecure.auth.config.RsaKeyConfig;
import com.shopsecure.auth.config.SecurityConfig;
import com.shopsecure.auth.service.AuthService;
import com.shopsecure.auth.service.UserService;
import com.shopsecure.auth.web.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Set;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Learn to TEST security rules: anonymous vs authenticated vs wrong role. */
@WebMvcTest(controllers = {AuthController.class, AdminUserController.class},
        properties = {"spring.cloud.config.enabled=false", "eureka.client.enabled=false"})
@Import({SecurityConfig.class, RsaKeyConfig.class, AuthControllerSecurityTest.PropsConfig.class})
class AuthControllerSecurityTest {

    @TestConfiguration
    @EnableConfigurationProperties(AppProperties.class)
    static class PropsConfig {}

    @Autowired MockMvc mvc;
    @MockBean AuthService authService;
    @MockBean UserService userService;
    @MockBean UserDetailsService userDetailsService;

    @Test
    void meRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void meWorksWithJwt() throws Exception {
        given(userService.getByUsername("john")).willReturn(
                new UserResponse(2L, "john", "john@x.dev", Set.of("USER"), true, false, Instant.now()));
        mvc.perform(get("/api/auth/me").with(jwt().jwt(j -> j.subject("john"))))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.username").value("john"));
    }

    @Test
    void adminEndpointForbiddenForUser() throws Exception {
        mvc.perform(get("/api/auth/admin/users").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
           .andExpect(status().isForbidden());
    }

    @Test
    void adminEndpointAllowedForAdmin() throws Exception {
        mvc.perform(get("/api/auth/admin/users").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
           .andExpect(status().isOk());
    }

    @Test
    void weakPasswordIsRejected() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"bob\",\"email\":\"bob@x.dev\",\"password\":\"password\"}"))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.errors.password").exists());
    }
}

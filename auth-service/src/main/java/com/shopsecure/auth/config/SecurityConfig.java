package com.shopsecure.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * The SecurityFilterChain is the core of Spring Security: every HTTP request passes through
 * an ordered list of filters (CORS, CSRF, BearerTokenAuthenticationFilter, AuthorizationFilter...).
 * This bean decides which filters run and what each URL requires.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // enables @PreAuthorize / @PostAuthorize on methods
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF protection is for cookie-authenticated browser sessions. Our APIs use
            // Authorization: Bearer headers which browsers never attach automatically.
            // The refresh cookie is protected by SameSite=Strict + path=/api/auth instead.
            .csrf(csrf -> csrf.disable())
            // STATELESS: no HttpSession, no JSESSIONID. Every request carries its own JWT.
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST,
                        "/api/auth/register", "/api/auth/login", "/api/auth/refresh",
                        "/api/auth/logout", "/api/auth/service-token").permitAll()
                .requestMatchers("/.well-known/jwks.json", "/actuator/health/**", "/actuator/info", "/error").permitAll()
                .requestMatchers("/api/auth/admin/**").hasRole("ADMIN")   // URL-level rule
                .anyRequest().authenticated())
            // Turns this service into an OAuth2 Resource Server: validates "Authorization: Bearer <jwt>"
            .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
            .headers(h -> h
                .frameOptions(f -> f.deny())
                .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'")));
        return http.build();
    }

    /** Maps the JWT "roles" claim ["ADMIN","USER"] to authorities ROLE_ADMIN, ROLE_USER. */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    /**
     * BCrypt: slow, salted, adaptive hash. Strength 12 = 2^12 rounds.
     * Never store plain or fast-hashed (MD5/SHA) passwords.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /** Username/password authentication used by the /login endpoint. */
    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(encoder);
        // Default: UsernameNotFoundException is converted to BadCredentialsException,
        // so attackers can't tell "no such user" from "wrong password" (prevents user enumeration).
        return new ProviderManager(provider);
    }
}

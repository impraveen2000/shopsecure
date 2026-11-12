package com.shopsecure.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Service registry. Every service registers here and looks up others by name
 * (e.g. "lb://product-service" in the gateway).
 * <p>
 * Security lesson: an open registry lets anyone register a fake "auth-service" and steal traffic.
 * So we require HTTP Basic auth. Clients use http://eureka:eureka-secret@host:18761/eureka.
 * CSRF is ignored only for /eureka/** because Eureka clients are machines, not browsers.
 */
@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServerApplication.class, args);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.ignoringRequestMatchers("/eureka/**"))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health/**").permitAll()
                .anyRequest().authenticated())
            .httpBasic(Customizer.withDefaults())
            .formLogin(Customizer.withDefaults()); // lets you log in to the dashboard from a browser
        return http.build();
    }
}

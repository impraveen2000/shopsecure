package com.shopsecure.auth.config;

import com.shopsecure.auth.domain.Role;
import com.shopsecure.auth.domain.User;
import com.shopsecure.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

/** Creates demo accounts on first start: admin / Admin@123 and john / User@1234. */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AppProperties props;

    public DataSeeder(UserRepository users, PasswordEncoder encoder, AppProperties props) {
        this.users = users;
        this.encoder = encoder;
        this.props = props;
    }

    @Override
    public void run(String... args) {
        create(props.admin().username(), props.admin().email(), props.admin().password(), Set.of(Role.ADMIN, Role.USER));
        create("john", "john@shopsecure.dev", "User@1234", Set.of(Role.USER));
    }

    private void create(String username, String email, String rawPassword, Set<Role> roles) {
        if (users.existsByUsername(username)) return;
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(encoder.encode(rawPassword));
        user.setRoles(roles);
        users.save(user);
        log.info("Seeded user '{}' with roles {}", username, roles);
    }
}

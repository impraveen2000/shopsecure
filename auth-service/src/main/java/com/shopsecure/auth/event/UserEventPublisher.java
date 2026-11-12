package com.shopsecure.auth.event;

import com.shopsecure.auth.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Publishes domain events to Kafka. notification-service consumes them asynchronously,
 * so registration doesn't fail or slow down if email sending is broken.
 * Note: events contain no password or token data.
 */
@Component
public class UserEventPublisher {

    public static final String TOPIC = "user-registered";
    private static final Logger log = LoggerFactory.getLogger(UserEventPublisher.class);

    private final KafkaTemplate<String, Object> kafka;

    public UserEventPublisher(KafkaTemplate<String, Object> kafka) {
        this.kafka = kafka;
    }

    public record UserRegisteredEvent(Long userId, String username, String email, Instant occurredAt) {}

    public void userRegistered(User user) {
        UserRegisteredEvent event = new UserRegisteredEvent(user.getId(), user.getUsername(), user.getEmail(), Instant.now());
        try {
            kafka.send(TOPIC, String.valueOf(user.getId()), event)
                 .whenComplete((result, ex) -> {
                     if (ex != null) log.warn("Could not publish UserRegisteredEvent: {}", ex.getMessage());
                 });
        } catch (Exception e) {
            log.warn("Kafka unavailable, UserRegisteredEvent not sent: {}", e.getMessage());
        }
    }
}

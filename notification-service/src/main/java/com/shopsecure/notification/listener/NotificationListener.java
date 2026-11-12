package com.shopsecure.notification.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopsecure.notification.store.NotificationStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * EVENT-DRIVEN communication: auth-service and order-service don't call this service directly.
 * They publish events to Kafka and move on; we react when we receive them.
 * If this service is down, events wait in Kafka and are processed when it comes back.
 */
@Component
public class NotificationListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

    private final ObjectMapper mapper;
    private final NotificationStore store;

    public NotificationListener(ObjectMapper mapper, NotificationStore store) {
        this.mapper = mapper;
        this.store = store;
    }

    @KafkaListener(topics = "user-registered", groupId = "notification-service")
    public void onUserRegistered(String payload) throws Exception {
        JsonNode event = mapper.readTree(payload);
        String username = event.path("username").asText();
        String email = event.path("email").asText();
        store.add(username, email, "Welcome to ShopSecure!",
                "Hi " + username + ", your account was created successfully.");
        log.info("[EMAIL] Welcome email sent to {}", mask(email));
    }

    @KafkaListener(topics = "order-events", groupId = "notification-service")
    public void onOrderEvent(String payload) throws Exception {
        JsonNode event = mapper.readTree(payload);
        String type = event.path("type").asText();
        String username = event.path("username").asText();
        String email = event.path("email").asText();
        long orderId = event.path("orderId").asLong();
        String total = event.path("total").asText();

        String subject = switch (type) {
            case "ORDER_PLACED" -> "Order #" + orderId + " confirmed";
            case "ORDER_CANCELLED" -> "Order #" + orderId + " cancelled";
            default -> "Order #" + orderId + " update";
        };
        store.add(username, email, subject, "Order #" + orderId + " total: " + total);
        log.info("[EMAIL] '{}' sent to {}", subject, mask(email));
    }

    /** Security lesson: don't write full personal data (PII) into logs. */
    private static String mask(String email) {
        int at = email.indexOf('@');
        if (at <= 1) return "***";
        return email.charAt(0) + "***" + email.substring(at);
    }
}

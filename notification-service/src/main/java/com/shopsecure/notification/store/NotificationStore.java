package com.shopsecure.notification.store;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * In-memory store of "sent" notifications (keeps the last 500).
 * Exercise: replace with a database or MongoDB and send real emails via spring-boot-starter-mail.
 */
@Component
public class NotificationStore {

    private static final int MAX = 500;
    private final Deque<Notification> items = new ConcurrentLinkedDeque<>();

    public record Notification(String id, String username, String email, String subject, String message, Instant createdAt) {}

    public Notification add(String username, String email, String subject, String message) {
        Notification n = new Notification(UUID.randomUUID().toString(), username, email, subject, message, Instant.now());
        items.addFirst(n);
        while (items.size() > MAX) items.pollLast();
        return n;
    }

    public List<Notification> forUser(String username) {
        return items.stream().filter(n -> n.username().equals(username)).toList();
    }

    public List<Notification> all() {
        return new ArrayList<>(items);
    }
}

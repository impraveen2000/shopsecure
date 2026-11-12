package com.shopsecure.notification.web;

import com.shopsecure.notification.store.NotificationStore;
import com.shopsecure.notification.store.NotificationStore.Notification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationStore store;

    public NotificationController(NotificationStore store) {
        this.store = store;
    }

    /** Users only ever see their own notifications: the filter key comes from the token. */
    @GetMapping("/my")
    public List<Notification> mine(@AuthenticationPrincipal Jwt jwt) {
        return store.forUser(jwt.getSubject());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<Notification> all() {
        return store.all();
    }
}

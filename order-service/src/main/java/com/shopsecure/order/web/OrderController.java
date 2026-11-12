package com.shopsecure.order.web;

import com.shopsecure.order.service.OrderService;
import com.shopsecure.order.web.dto.OrderDtos.CreateOrderRequest;
import com.shopsecure.order.web.dto.OrderDtos.OrderResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('USER')")
    public OrderResponse place(@Valid @RequestBody CreateOrderRequest request, @AuthenticationPrincipal Jwt jwt) {
        return service.placeOrder(request, jwt);
    }

    @GetMapping("/my")
    public List<OrderResponse> myOrders(@AuthenticationPrincipal Jwt jwt) {
        return service.myOrders(jwt.getSubject());
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable Long id) {
        return service.getOrder(id);          // ownership enforced by @PostAuthorize
    }

    @GetMapping
    public List<OrderResponse> all() {
        return service.allOrders();           // ADMIN only, enforced by @PreAuthorize
    }

    @PutMapping("/{id}/cancel")
    public OrderResponse cancel(@PathVariable Long id, Authentication authentication) {
        return service.cancel(id, authentication);
    }
}

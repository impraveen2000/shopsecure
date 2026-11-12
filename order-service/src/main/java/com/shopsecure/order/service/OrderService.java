package com.shopsecure.order.service;

import com.shopsecure.order.client.ClientDtos.ProductDto;
import com.shopsecure.order.client.ProductGateway;
import com.shopsecure.order.domain.Order;
import com.shopsecure.order.domain.OrderItem;
import com.shopsecure.order.domain.OrderStatus;
import com.shopsecure.order.event.OrderEventPublisher;
import com.shopsecure.order.exception.BusinessException;
import com.shopsecure.order.exception.NotFoundException;
import com.shopsecure.order.repository.OrderRepository;
import com.shopsecure.order.web.dto.OrderDtos.CreateOrderRequest;
import com.shopsecure.order.web.dto.OrderDtos.OrderItemRequest;
import com.shopsecure.order.web.dto.OrderDtos.OrderResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orders;
    private final ProductGateway products;
    private final OrderEventPublisher events;

    public OrderService(OrderRepository orders, ProductGateway products, OrderEventPublisher events) {
        this.orders = orders;
        this.products = products;
        this.events = events;
    }

    /**
     * SAGA (orchestration, simplified):
     *   1. reserve stock for each product in product-service (remote call)
     *   2. save the order locally
     *   3. if anything fails, run COMPENSATING actions (release what was reserved)
     * There is no distributed transaction across services: each service owns its own database.
     */
    public OrderResponse placeOrder(CreateOrderRequest request, Jwt jwt) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (OrderItemRequest item : request.items()) {
            quantities.merge(item.productId(), item.quantity(), Integer::sum);
        }
        if (quantities.values().stream().anyMatch(q -> q > 100)) {
            throw new BusinessException("Maximum 100 units per product");
        }

        Order order = new Order();
        Object uid = jwt.getClaim("uid");
        order.setUserId(uid instanceof Number n ? n.longValue() : null);
        order.setUsername(jwt.getSubject());
        order.setEmail(jwt.getClaimAsString("email"));

        List<Map.Entry<Long, Integer>> reserved = new ArrayList<>();
        try {
            for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
                ProductDto product = products.reserve(entry.getKey(), entry.getValue());
                reserved.add(entry);
                order.addItem(new OrderItem(product.id(), product.name(), product.price(), entry.getValue()));
            }
            order.setTotal(order.getItems().stream().map(OrderItem::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add));
            order.setStatus(OrderStatus.CONFIRMED);
            Order saved = orders.save(order);
            events.publish("ORDER_PLACED", saved);
            log.info("Order {} placed by '{}' total {}", saved.getId(), saved.getUsername(), saved.getTotal());
            return OrderResponse.from(saved);
        } catch (RuntimeException ex) {
            if (!reserved.isEmpty()) {
                log.warn("Order failed ({}), compensating {} reservation(s)", ex.getMessage(), reserved.size());
                reserved.forEach(e -> products.release(e.getKey(), e.getValue()));
            }
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> myOrders(String username) {
        return orders.findByUsernameOrderByCreatedAtDesc(username).stream().map(OrderResponse::from).toList();
    }

    /**
     * @PostAuthorize runs AFTER the method and checks the returned object.
     * Prevents IDOR (Insecure Direct Object Reference): user "john" cannot read order 42 of "alice"
     * just by changing the id in the URL.
     */
    @Transactional(readOnly = true)
    @PostAuthorize("returnObject.username() == authentication.name or hasRole('ADMIN')")
    public OrderResponse getOrder(Long id) {
        return orders.findWithItemsById(id).map(OrderResponse::from)
                .orElseThrow(() -> new NotFoundException("Order " + id + " not found"));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public List<OrderResponse> allOrders() {
        return orders.findAllByOrderByCreatedAtDesc().stream().map(OrderResponse::from).toList();
    }

    /** Programmatic ownership check (alternative to @PostAuthorize when you must check BEFORE acting). */
    public OrderResponse cancel(Long id, Authentication auth) {
        Order order = orders.findWithItemsById(id)
                .orElseThrow(() -> new NotFoundException("Order " + id + " not found"));

        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (!isAdmin && !order.getUsername().equals(auth.getName())) {
            throw new AccessDeniedException("You can only cancel your own orders");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException("Order is already cancelled");
        }

        order.getItems().forEach(i -> products.release(i.getProductId(), i.getQuantity()));
        order.setStatus(OrderStatus.CANCELLED);
        orders.save(order);
        events.publish("ORDER_CANCELLED", order);   // 'order' already has its items loaded
        return OrderResponse.from(order);
    }
}

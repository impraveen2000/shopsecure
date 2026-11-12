package com.shopsecure.order.web.dto;

import com.shopsecure.order.domain.Order;
import com.shopsecure.order.domain.OrderItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class OrderDtos {

    private OrderDtos() {}

    /** Notice: no price, no userId, no status. The client only says WHAT it wants. */
    public record CreateOrderRequest(@NotEmpty @Size(max = 20) List<@Valid OrderItemRequest> items) {}

    public record OrderItemRequest(@NotNull Long productId, @Min(1) @Max(100) int quantity) {}

    public record OrderItemResponse(Long productId, String productName, BigDecimal unitPrice, int quantity,
                                    BigDecimal subtotal) {
        static OrderItemResponse from(OrderItem i) {
            return new OrderItemResponse(i.getProductId(), i.getProductName(), i.getUnitPrice(), i.getQuantity(), i.subtotal());
        }
    }

    public record OrderResponse(Long id, String username, String status, BigDecimal total,
                                List<OrderItemResponse> items, Instant createdAt) {
        public static OrderResponse from(Order o) {
            return new OrderResponse(o.getId(), o.getUsername(), o.getStatus().name(), o.getTotal(),
                    o.getItems().stream().map(OrderItemResponse::from).toList(), o.getCreatedAt());
        }
    }
}

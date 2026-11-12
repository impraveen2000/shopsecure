package com.shopsecure.order.client;

import java.math.BigDecimal;

/** Only the fields order-service needs from other services (unknown JSON fields are ignored). */
public final class ClientDtos {

    private ClientDtos() {}

    public record ProductDto(Long id, String name, BigDecimal price, int stock) {}

    public record StockRequest(int quantity) {}

    public record ServiceTokenRequest(String clientId, String clientSecret) {}

    public record TokenResponse(String accessToken, String tokenType, long expiresIn) {}
}

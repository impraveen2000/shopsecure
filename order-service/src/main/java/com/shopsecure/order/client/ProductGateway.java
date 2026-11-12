package com.shopsecure.order.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopsecure.order.client.ClientDtos.ProductDto;
import com.shopsecure.order.client.ClientDtos.StockRequest;
import com.shopsecure.order.exception.BusinessException;
import com.shopsecure.order.exception.NotFoundException;
import com.shopsecure.order.exception.ServiceUnavailableException;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Wraps ProductClient with a CIRCUIT BREAKER.
 * <p>
 * CLOSED   -> calls pass through, failures are counted.
 * OPEN     -> after 50% failures, calls fail immediately for 10s (protects both services).
 * HALF_OPEN-> a few trial calls decide whether to close again.
 * Watch it live: GET http://localhost:18083/actuator/health (circuitBreakers section).
 */
@Component
public class ProductGateway {

    private static final Logger log = LoggerFactory.getLogger(ProductGateway.class);

    private final ProductClient client;
    private final ServiceTokenProvider tokens;
    private final ObjectMapper mapper;

    public ProductGateway(ProductClient client, ServiceTokenProvider tokens, ObjectMapper mapper) {
        this.client = client;
        this.tokens = tokens;
        this.mapper = mapper;
    }

    @CircuitBreaker(name = "productService", fallbackMethod = "reserveFallback")
    public ProductDto reserve(Long productId, int quantity) {
        try {
            return client.reserve(productId, new StockRequest(quantity));
        } catch (FeignException.Unauthorized e) {
            tokens.invalidate();     // cached token rejected: get a fresh one and retry once
            return client.reserve(productId, new StockRequest(quantity));
        }
    }

    @CircuitBreaker(name = "productService", fallbackMethod = "releaseFallback")
    public void release(Long productId, int quantity) {
        client.release(productId, new StockRequest(quantity));
    }

    public ProductDto reserveFallback(Long productId, int quantity, Throwable t) {
        throw translate(t, productId);
    }

    public void releaseFallback(Long productId, int quantity, Throwable t) {
        // In a real system: write to an outbox / retry queue so stock is eventually corrected.
        log.error("COMPENSATION FAILED: could not release {} x product {}: {}", quantity, productId, t.toString());
    }

    private RuntimeException translate(Throwable t, Long productId) {
        if (t instanceof FeignException.NotFound) {
            return new NotFoundException("Product " + productId + " not found");
        }
        if (t instanceof FeignException.Conflict conflict) {
            return new BusinessException(detail(conflict));
        }
        if (t instanceof FeignException.Forbidden || t instanceof FeignException.Unauthorized) {
            log.error("order-service is not authorized to call product-service: {}", t.getMessage());
            return new ServiceUnavailableException("Ordering is temporarily unavailable");
        }
        if (t instanceof CallNotPermittedException) {
            return new ServiceUnavailableException("Product service is unavailable (circuit open). Try again shortly.");
        }
        log.warn("Product service call failed: {}", t.toString());
        return new ServiceUnavailableException("Product service is unavailable. Try again shortly.");
    }

    private String detail(FeignException e) {
        try {
            return mapper.readTree(e.contentUTF8()).path("detail").asText("Request conflicts with current state");
        } catch (Exception ex) {
            return "Request conflicts with current state";
        }
    }
}

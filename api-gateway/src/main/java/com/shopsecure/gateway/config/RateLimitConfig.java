package com.shopsecure.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

/**
 * RATE LIMITING decides "who" a request belongs to via a KeyResolver; Redis keeps a token bucket per key.
 * Over the limit -> HTTP 429 Too Many Requests.
 * <p>
 * Note: we use the TCP remote address. Behind a load balancer you would read X-Forwarded-For, but ONLY
 * if you trust the proxy that sets it, otherwise attackers can spoof the header to dodge limits.
 */
@Configuration
public class RateLimitConfig {

    @Bean
    @Primary
    public KeyResolver userOrIpKeyResolver() {
        return exchange -> exchange.getPrincipal()
                .map(p -> "user:" + p.getName())
                .switchIfEmpty(Mono.fromSupplier(() -> "anon:" + clientIp(exchange)));
    }

    @Bean
    public KeyResolver loginIpKeyResolver() {
        return exchange -> Mono.just("auth:" + clientIp(exchange));
    }

    private static String clientIp(ServerWebExchange exchange) {
        InetSocketAddress address = exchange.getRequest().getRemoteAddress();
        return address != null && address.getAddress() != null ? address.getAddress().getHostAddress() : "unknown";
    }
}

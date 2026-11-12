package com.shopsecure.order.client;

import com.shopsecure.order.client.ClientDtos.ServiceTokenRequest;
import com.shopsecure.order.client.ClientDtos.TokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * SERVICE-TO-SERVICE AUTHENTICATION (client-credentials pattern)
 * <p>
 * order-service authenticates as ITSELF (clientId + secret) and receives a short-lived JWT with
 * role SERVICE. product-service only allows stock changes for that role, so an end user can never
 * call /reserve directly, even with a valid user token.
 * <p>
 * The token is cached and renewed 30s before expiry to avoid calling auth-service on every request.
 */
@Component
public class ServiceTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(ServiceTokenProvider.class);

    private final AuthClient authClient;
    private final String clientId;
    private final String clientSecret;

    private String token;
    private Instant expiresAt = Instant.EPOCH;

    public ServiceTokenProvider(AuthClient authClient,
                                @Value("${app.service-auth.client-id}") String clientId,
                                @Value("${app.service-auth.client-secret}") String clientSecret) {
        this.authClient = authClient;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public synchronized String getToken() {
        if (token == null || Instant.now().isAfter(expiresAt.minusSeconds(30))) {
            TokenResponse response = authClient.serviceToken(new ServiceTokenRequest(clientId, clientSecret));
            token = response.accessToken();
            expiresAt = Instant.now().plusSeconds(response.expiresIn());
            log.info("Obtained new SERVICE token for '{}', valid for {}s", clientId, response.expiresIn());
        }
        return token;
    }

    /** Used when a call gets 401 (e.g. auth-service restarted with new signing keys). */
    public synchronized void invalidate() {
        token = null;
    }
}

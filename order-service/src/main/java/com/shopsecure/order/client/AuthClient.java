package com.shopsecure.order.client;

import com.shopsecure.order.client.ClientDtos.ServiceTokenRequest;
import com.shopsecure.order.client.ClientDtos.TokenResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** "auth-service" is resolved through Eureka; the call goes service-to-service, not through the gateway. */
@FeignClient(name = "auth-service", contextId = "authClient")
public interface AuthClient {

    @PostMapping("/api/auth/service-token")
    TokenResponse serviceToken(@RequestBody ServiceTokenRequest request);
}

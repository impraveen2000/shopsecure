package com.shopsecure.order.client;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;

/**
 * Deliberately NOT annotated with @Configuration: otherwise component scanning would apply this
 * interceptor to EVERY Feign client (including AuthClient, which must not need a token).
 * It is only used through @FeignClient(configuration = ServiceAuthFeignConfig.class).
 */
public class ServiceAuthFeignConfig {

    @Bean
    public RequestInterceptor serviceTokenInterceptor(ServiceTokenProvider tokenProvider) {
        return template -> template.header("Authorization", "Bearer " + tokenProvider.getToken());
    }
}

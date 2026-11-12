package com.shopsecure.order.client;

import com.shopsecure.order.client.ClientDtos.ProductDto;
import com.shopsecure.order.client.ClientDtos.StockRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Every call made through this client gets "Authorization: Bearer <SERVICE token>"
 * added by {@link ServiceAuthFeignConfig}.
 */
@FeignClient(name = "product-service", contextId = "productClient", configuration = ServiceAuthFeignConfig.class)
public interface ProductClient {

    @GetMapping("/api/products/{id}")
    ProductDto getProduct(@PathVariable("id") Long id);

    @PostMapping("/api/products/{id}/reserve")
    ProductDto reserve(@PathVariable("id") Long id, @RequestBody StockRequest request);

    @PostMapping("/api/products/{id}/release")
    ProductDto release(@PathVariable("id") Long id, @RequestBody StockRequest request);
}

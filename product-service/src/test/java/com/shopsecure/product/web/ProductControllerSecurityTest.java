package com.shopsecure.product.web;

import com.shopsecure.product.config.SecurityConfig;
import com.shopsecure.product.service.ProductService;
import com.shopsecure.product.web.dto.ProductDtos.ProductResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProductController.class,
        properties = {"spring.cloud.config.enabled=false", "eureka.client.enabled=false"})
@Import(SecurityConfig.class)
class ProductControllerSecurityTest {

    private static final String BODY = """
            {"name":"Test","description":"d","price":10.00,"stock":5,"category":"Books"}""";

    @Autowired MockMvc mvc;
    @MockBean ProductService service;

    private ProductResponse sample() {
        return new ProductResponse(1L, "Test", "d", new BigDecimal("10.00"), 5, "Books", null, "admin", Instant.now());
    }

    @Test
    void anyoneCanBrowseProducts() throws Exception {
        given(service.get(1L)).willReturn(sample());
        mvc.perform(get("/api/products/1")).andExpect(status().isOk());
    }

    @Test
    void createRequiresAuthentication() throws Exception {
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(BODY))
           .andExpect(status().isUnauthorized());
    }

    @Test
    void userCannotCreateProducts() throws Exception {
        mvc.perform(post("/api/products").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
           .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateProducts() throws Exception {
        given(service.create(any(), eq("admin"))).willReturn(sample());
        mvc.perform(post("/api/products")
                .with(jwt().jwt(j -> j.subject("admin")).authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
           .andExpect(status().isCreated());
    }

    @Test
    void adminCannotReserveStock_onlyServicesCan() throws Exception {
        mvc.perform(post("/api/products/1/reserve").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":1}"))
           .andExpect(status().isForbidden());
    }

    @Test
    void serviceCanReserveStock() throws Exception {
        given(service.reserve(anyLong(), anyInt())).willReturn(sample());
        mvc.perform(post("/api/products/1/reserve").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SERVICE")))
                .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":1}"))
           .andExpect(status().isOk());
    }
}

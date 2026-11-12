package com.shopsecure.product.web.dto;

import com.shopsecure.product.domain.Product;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class ProductDtos {

    private ProductDtos() {}

    public record ProductRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 2000) String description,
            @NotNull @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2) BigDecimal price,
            @Min(0) @Max(1_000_000) int stock,
            @Size(max = 100) String category,
            @Size(max = 500) @Pattern(regexp = "^(https?://.*)?$", message = "Must be an http(s) URL") String imageUrl
    ) {}

    public record ProductResponse(Long id, String name, String description, BigDecimal price, int stock,
                                  String category, String imageUrl, String createdBy, Instant createdAt) {
        public static ProductResponse from(Product p) {
            return new ProductResponse(p.getId(), p.getName(), p.getDescription(), p.getPrice(), p.getStock(),
                    p.getCategory(), p.getImageUrl(), p.getCreatedBy(), p.getCreatedAt());
        }
    }

    public record StockRequest(@Min(1) @Max(100) int quantity) {}

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
        public static <T> PageResponse<T> of(Page<T> page) {
            return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                    page.getTotalElements(), page.getTotalPages());
        }
    }
}

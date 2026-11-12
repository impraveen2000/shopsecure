package com.shopsecure.product.web;

import com.shopsecure.product.service.ProductService;
import com.shopsecure.product.web.dto.ProductDtos.PageResponse;
import com.shopsecure.product.web.dto.ProductDtos.ProductRequest;
import com.shopsecure.product.web.dto.ProductDtos.ProductResponse;
import com.shopsecure.product.web.dto.ProductDtos.StockRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * METHOD SECURITY examples:
 *   GET               -> public (see SecurityConfig)
 *   POST/PUT/DELETE   -> hasRole('ADMIN')     (human admins)
 *   reserve/release   -> hasRole('SERVICE')   (only other microservices, never end users)
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    /** Whitelist of sortable fields: never let clients sort/filter on arbitrary columns. */
    private static final Set<String> SORTABLE = Set.of("id", "name", "price", "createdAt");

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<ProductResponse> list(@RequestParam(required = false) String q,
                                              @RequestParam(required = false) String category,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "12") int size,
                                              @RequestParam(defaultValue = "id") String sort) {
        String sortField = SORTABLE.contains(sort) ? sort : "id";
        int safeSize = Math.min(Math.max(size, 1), 100);     // prevent "size=1000000" abuse
        return service.search(q, category, PageRequest.of(Math.max(page, 0), safeSize, Sort.by(sortField)));
    }

    @GetMapping("/categories")
    public List<String> categories() {
        return service.categories();
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ProductResponse create(@Valid @RequestBody ProductRequest request, @AuthenticationPrincipal Jwt jwt) {
        return service.create(request, jwt.getSubject());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @PostMapping("/{id}/reserve")
    @PreAuthorize("hasRole('SERVICE')")
    public ProductResponse reserve(@PathVariable Long id, @Valid @RequestBody StockRequest request) {
        return service.reserve(id, request.quantity());
    }

    @PostMapping("/{id}/release")
    @PreAuthorize("hasRole('SERVICE')")
    public ProductResponse release(@PathVariable Long id, @Valid @RequestBody StockRequest request) {
        return service.release(id, request.quantity());
    }
}

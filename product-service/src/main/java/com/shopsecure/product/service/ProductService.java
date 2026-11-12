package com.shopsecure.product.service;

import com.shopsecure.product.domain.Product;
import com.shopsecure.product.exception.InsufficientStockException;
import com.shopsecure.product.exception.NotFoundException;
import com.shopsecure.product.repository.ProductRepository;
import com.shopsecure.product.web.dto.ProductDtos.PageResponse;
import com.shopsecure.product.web.dto.ProductDtos.ProductRequest;
import com.shopsecure.product.web.dto.ProductDtos.ProductResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);
    private final ProductRepository products;

    public ProductService(ProductRepository products) {
        this.products = products;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(String q, String category, Pageable pageable) {
        Specification<Product> spec = Specification.where(null);
        if (StringUtils.hasText(q)) {
            String like = "%" + q.toLowerCase() + "%";   // bound as a parameter, not concatenated into SQL
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("name")), like));
        }
        if (StringUtils.hasText(category)) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("category"), category));
        }
        return PageResponse.of(products.findAll(spec, pageable).map(ProductResponse::from));
    }

    @Transactional(readOnly = true)
    public ProductResponse get(Long id) {
        return ProductResponse.from(load(id));
    }

    @Transactional(readOnly = true)
    public List<String> categories() {
        return products.findCategories();
    }

    @Transactional
    public ProductResponse create(ProductRequest request, String createdBy) {
        Product p = new Product();
        apply(p, request);
        p.setCreatedBy(createdBy);
        return ProductResponse.from(products.save(p));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product p = load(id);
        apply(p, request);
        return ProductResponse.from(p);
    }

    @Transactional
    public void delete(Long id) {
        products.delete(load(id));
    }

    /** Called by order-service (with a SERVICE token) while placing an order. */
    @Transactional
    public ProductResponse reserve(Long id, int quantity) {
        Product p = load(id);
        if (p.getStock() < quantity) {
            throw new InsufficientStockException("Only " + p.getStock() + " left of '" + p.getName() + "'");
        }
        p.setStock(p.getStock() - quantity);
        log.info("Reserved {} x product {} (stock now {})", quantity, id, p.getStock());
        return ProductResponse.from(p);
    }

    /** Compensating action (saga): give stock back when an order fails or is cancelled. */
    @Transactional
    public ProductResponse release(Long id, int quantity) {
        Product p = load(id);
        p.setStock(p.getStock() + quantity);
        log.info("Released {} x product {} (stock now {})", quantity, id, p.getStock());
        return ProductResponse.from(p);
    }

    private Product load(Long id) {
        return products.findById(id).orElseThrow(() -> new NotFoundException("Product " + id + " not found"));
    }

    private void apply(Product p, ProductRequest r) {
        p.setName(r.name());
        p.setDescription(r.description());
        p.setPrice(r.price());
        p.setStock(r.stock());
        p.setCategory(r.category());
        p.setImageUrl(r.imageUrl());
    }
}

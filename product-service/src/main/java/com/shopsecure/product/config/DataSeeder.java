package com.shopsecure.product.config;

import com.shopsecure.product.domain.Product;
import com.shopsecure.product.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final ProductRepository products;

    public DataSeeder(ProductRepository products) {
        this.products = products;
    }

    @Override
    public void run(String... args) {
        if (products.count() > 0) return;
        products.saveAll(List.of(
            p("Mechanical Keyboard", "RGB, hot-swappable switches", "799.00", 25, "Electronics"),
            p("Wireless Mouse", "Ergonomic, 6 buttons", "349.00", 40, "Electronics"),
            p("4K Monitor 27\"", "IPS panel, USB-C", "2899.00", 10, "Electronics"),
            p("Noise Cancelling Headphones", "40h battery", "1499.00", 15, "Electronics"),
            p("Spring Security in Action", "Book, 2nd edition", "499.00", 30, "Books"),
            p("Microservices Patterns", "Book by Chris Richardson", "549.00", 20, "Books"),
            p("Standing Desk", "Electric, memory presets", "4999.00", 5, "Furniture"),
            p("Office Chair", "Lumbar support, mesh back", "1999.00", 8, "Furniture")
        ));
    }

    private Product p(String name, String desc, String price, int stock, String category) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(desc);
        product.setPrice(new BigDecimal(price));
        product.setStock(stock);
        product.setCategory(category);
        product.setCreatedBy("seed");
        return product;
    }
}

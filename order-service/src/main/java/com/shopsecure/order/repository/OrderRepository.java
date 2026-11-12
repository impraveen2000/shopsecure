package com.shopsecure.order.repository;

import com.shopsecure.order.domain.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /** @EntityGraph loads items in the same query (avoids the N+1 problem and lazy-loading errors). */
    @EntityGraph(attributePaths = "items")
    List<Order> findByUsernameOrderByCreatedAtDesc(String username);

    @EntityGraph(attributePaths = "items")
    List<Order> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "items")
    Optional<Order> findWithItemsById(Long id);
}

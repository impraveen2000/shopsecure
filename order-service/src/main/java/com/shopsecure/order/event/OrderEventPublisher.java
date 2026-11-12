package com.shopsecure.order.event;

import com.shopsecure.order.domain.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;

@Component
public class OrderEventPublisher {

    public static final String TOPIC = "order-events";
    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final KafkaTemplate<String, Object> kafka;

    public OrderEventPublisher(KafkaTemplate<String, Object> kafka) {
        this.kafka = kafka;
    }

    public record OrderEvent(String type, Long orderId, String username, String email,
                             BigDecimal total, int itemCount, Instant occurredAt) {}

    public void publish(String type, Order order) {
        OrderEvent event = new OrderEvent(type, order.getId(), order.getUsername(), order.getEmail(),
                order.getTotal(), order.getItems().size(), Instant.now());
        try {
            kafka.send(TOPIC, String.valueOf(order.getId()), event)
                 .whenComplete((r, ex) -> {
                     if (ex != null) log.warn("Could not publish {}: {}", type, ex.getMessage());
                 });
        } catch (Exception e) {
            log.warn("Kafka unavailable, {} not sent: {}", type, e.getMessage());
        }
    }
}

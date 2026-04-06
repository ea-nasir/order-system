package com.example.ordersystem.orderservice.messaging;

import com.example.ordersystem.orderservice.logging.LogContext;
import com.example.ordersystem.orderservice.service.OrderService;
import com.example.ordersystem.sharedevents.InventoryRejectedEvent;
import com.example.ordersystem.sharedevents.OrderRejectedEvent;
import config.KafkaTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class InventoryRejectedConsumer {
    private static final Logger log = LoggerFactory.getLogger(InventoryRejectedConsumer.class);
    OrderService orderService;
    KafkaTemplate<String, Object> kafkaTemplate;

    public InventoryRejectedConsumer(OrderService orderService, KafkaTemplate<String, Object> kafkaTemplate) {
        this.orderService = orderService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = KafkaTopics.INVENTORY_REJECTED, groupId = "order.service") //todo: make groupids psfs
    public void consume(InventoryRejectedEvent inventoryRejectedEvent) {
        LogContext.put(
                inventoryRejectedEvent.orderId(),
                inventoryRejectedEvent.eventId().toString(),
                "InventoryRejectedEvent"
        );
        try {
            log.warn("Consumed inventory rejection: reason={}", inventoryRejectedEvent.reason());

            orderService.rejectOrder(
                    inventoryRejectedEvent.orderId()
            );
            log.warn("Rejected due to inventory failure: orderId={}", inventoryRejectedEvent.orderId());

            OrderRejectedEvent orderRejectedEvent = new OrderRejectedEvent(
                    UUID.randomUUID(),
                    Instant.now(),
                    inventoryRejectedEvent.orderId(),
                    "Order rejected due to insufficient inventory"
            );

            kafkaTemplate.send(
                    KafkaTopics.ORDERS_REJECTED,
                    inventoryRejectedEvent.orderId(),
                    orderRejectedEvent
            );

            log.info("Published event: topic={}, publishedEventType={}, publishedEventId={}, rejectionReason={}",
                    KafkaTopics.ORDERS_REJECTED,
                    "OrderRejectedEvent",
                    orderRejectedEvent.eventId(),
                    orderRejectedEvent.reason());
        } finally {
            LogContext.clear();
        }
    }
}

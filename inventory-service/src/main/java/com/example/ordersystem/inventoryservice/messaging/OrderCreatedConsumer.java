package com.example.ordersystem.inventoryservice.messaging;

import com.example.ordersystem.inventoryservice.logging.LogContext;
import com.example.ordersystem.inventoryservice.service.InventoryService;
import com.example.ordersystem.sharedevents.InventoryRejectedEvent;
import com.example.ordersystem.sharedevents.InventoryReservedEvent;
import com.example.ordersystem.sharedevents.OrderCreatedEvent;
import config.KafkaTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class OrderCreatedConsumer {
    private static final Logger log = LoggerFactory.getLogger(OrderCreatedConsumer.class);
    InventoryService inventoryService;
    KafkaTemplate<String, Object> kafkaTemplate;

    public OrderCreatedConsumer(InventoryService inventoryService, KafkaTemplate<String, Object> kafkaTemplate) {
        this.inventoryService = inventoryService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = KafkaTopics.ORDERS_CREATED, groupId = "inventory.service") //todo: make groupids psfs
    public void consume(OrderCreatedEvent event) {
        LogContext.put(
                event.orderId(),
                event.eventId().toString(),
                "OrderCreatedEvent"
        );
        try {
            log.info("Consumed order creation: productId={}, quantity={}, totalAmount={}",
                    event.productId(),
                    event.quantity(),
                    event.totalAmount());
            boolean reserved = inventoryService.reserve(
                    event.orderId(),
                    event.productId(),
                    event.quantity()
            );

            if (reserved) {
                kafkaTemplate.send(
                        KafkaTopics.INVENTORY_RESERVED,
                        event.orderId(),
                        new InventoryReservedEvent(
                                UUID.randomUUID(),
                                Instant.now(),
                                event.orderId(),
                                event.productId(),
                                event.quantity(),
                                event.totalAmount()
                        )
                );
            } else {
                kafkaTemplate.send(
                        KafkaTopics.INVENTORY_REJECTED,
                        event.orderId(),
                        new InventoryRejectedEvent(
                                UUID.randomUUID(),
                                Instant.now(),
                                event.orderId(),
                                "Insufficient stock or quantity < 1"
                        )
                );
            }
        } finally {
            LogContext.clear();
        }
    }
}

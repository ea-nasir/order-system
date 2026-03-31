package com.example.ordersystem.inventoryservice.messaging;

import com.example.ordersystem.inventoryservice.service.InventoryService;
import com.example.ordersystem.sharedevents.InventoryRejectedEvent;
import com.example.ordersystem.sharedevents.InventoryReservedEvent;
import com.example.ordersystem.sharedevents.OrderCreatedEvent;
import config.KafkaTopics;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class OrderCreatedConsumer {
    InventoryService inventoryService;
    KafkaTemplate<String,Object> kafkaTemplate;

    public OrderCreatedConsumer(InventoryService inventoryService, KafkaTemplate<String,Object> kafkaTemplate){
        this.inventoryService = inventoryService;
        this.kafkaTemplate = kafkaTemplate;
    }
    @KafkaListener(topics = KafkaTopics.ORDERS_CREATED,groupId = "inventory.service.v2")
    public void consume(OrderCreatedEvent event){
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
                            event.quantity()
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
    }
}

package com.example.ordersystem.orderservice.messaging;

import com.example.ordersystem.orderservice.service.OrderService;
import com.example.ordersystem.sharedevents.InventoryRejectedEvent;
import com.example.ordersystem.sharedevents.OrderRejectedEvent;
import com.example.ordersystem.sharedevents.PaymentFailedEvent;
import config.KafkaTopics;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class InventoryRejectedConsumer {
    OrderService orderService;
    KafkaTemplate<String, Object> kafkaTemplate;

    public InventoryRejectedConsumer(OrderService orderService, KafkaTemplate<String, Object> kafkaTemplate) {
        this.orderService = orderService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = KafkaTopics.INVENTORY_REJECTED, groupId = "order.service") //todo: make groupids psfs
    public void consume(InventoryRejectedEvent inventoryRejectedEvent) {
        orderService.rejectOrder(
                inventoryRejectedEvent.orderId()
        );
        kafkaTemplate.send(
                KafkaTopics.ORDERS_REJECTED,
                inventoryRejectedEvent.orderId(),
                new OrderRejectedEvent(
                        UUID.randomUUID(),
                        Instant.now(),
                        inventoryRejectedEvent.orderId(),
                        "Order rejected due to insufficient inventory"
                )
        );
    }
}

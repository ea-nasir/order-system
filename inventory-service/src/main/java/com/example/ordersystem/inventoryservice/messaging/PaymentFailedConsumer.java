package com.example.ordersystem.inventoryservice.messaging;

import com.example.ordersystem.inventoryservice.service.InventoryService;
import com.example.ordersystem.sharedevents.PaymentFailedEvent;
import config.KafkaTopics;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentFailedConsumer {
    KafkaTemplate<String, Object> kafkaTemplate;
    InventoryService inventoryService;

    public PaymentFailedConsumer(KafkaTemplate<String, Object> kafkaTemplate, InventoryService inventoryService) {
        this.kafkaTemplate = kafkaTemplate;
        this.inventoryService = inventoryService;
    }

    @KafkaListener(topics = KafkaTopics.PAYMENTS_FAILED, groupId = "inventory.service")
    public void consume(PaymentFailedEvent paymentFailedEvent) {
        inventoryService.releaseReservation(paymentFailedEvent.orderId());
    }
}

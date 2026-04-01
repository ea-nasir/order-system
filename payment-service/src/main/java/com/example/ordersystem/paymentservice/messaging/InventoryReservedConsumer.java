package com.example.ordersystem.paymentservice.messaging;

import com.example.ordersystem.paymentservice.model.Payment;
import com.example.ordersystem.paymentservice.service.PaymentService;
import com.example.ordersystem.sharedevents.InventoryReservedEvent;
import com.example.ordersystem.sharedevents.PaymentAuthorizedEvent;
import com.example.ordersystem.sharedevents.PaymentFailedEvent;
import config.KafkaTopics;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class InventoryReservedConsumer {
    PaymentService paymentService;
    KafkaTemplate<String, Object> kafkaTemplate;

    public InventoryReservedConsumer(PaymentService paymentService, KafkaTemplate<String, Object> kafkaTemplate) {
        this.paymentService = paymentService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = KafkaTopics.INVENTORY_RESERVED, groupId = "payment.service")
    public void consume(InventoryReservedEvent inventoryReservedEvent) {
        String orderId = inventoryReservedEvent.orderId();
        boolean authorized = paymentService.authorize(new Payment(orderId, inventoryReservedEvent.totalAmount()));

        if (!authorized) {
            kafkaTemplate.send(KafkaTopics.PAYMENTS_FAILED, orderId, new PaymentFailedEvent(
                    UUID.randomUUID(),
                    Instant.now(),
                    orderId,
                    "Payment failed due to x issue" //todo: make better reasons
            ));
        }
        kafkaTemplate.send(KafkaTopics.PAYMENTS_AUTHORIZED, orderId, new PaymentAuthorizedEvent(
                UUID.randomUUID(),
                Instant.now(),
                orderId
        ));
    }


}

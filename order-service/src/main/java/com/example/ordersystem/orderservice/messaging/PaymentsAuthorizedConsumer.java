package com.example.ordersystem.orderservice.messaging;

import com.example.ordersystem.orderservice.service.OrderService;
import com.example.ordersystem.sharedevents.OrderConfirmedEvent;
import com.example.ordersystem.sharedevents.PaymentAuthorizedEvent;
import config.KafkaTopics;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;
@Component
public class PaymentsAuthorizedConsumer {
    OrderService orderService;
    KafkaTemplate<String, Object> kafkaTemplate;

    public PaymentsAuthorizedConsumer(OrderService orderService, KafkaTemplate<String, Object> kafkaTemplate) {
        this.orderService = orderService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = KafkaTopics.PAYMENTS_AUTHORIZED, groupId = "order.service") //todo: make groupids psfs
    public void consume(PaymentAuthorizedEvent paymentAuthorizedEvent) {
        orderService.confirmOrder(
                paymentAuthorizedEvent.orderId(),
                paymentAuthorizedEvent.occurredAt()
        );
        kafkaTemplate.send(
                KafkaTopics.ORDERS_CONFIRMED,
                paymentAuthorizedEvent.orderId(),
                new OrderConfirmedEvent(
                        UUID.randomUUID(),
                        Instant.now(),
                        paymentAuthorizedEvent.orderId()
                )
        );
    }
}

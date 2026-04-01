package com.example.ordersystem.orderservice.messaging;

import com.example.ordersystem.orderservice.service.OrderService;
import com.example.ordersystem.sharedevents.OrderConfirmedEvent;
import com.example.ordersystem.sharedevents.PaymentFailedEvent;
import config.KafkaTopics;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.UUID;

public class PaymentFailedConsumer {
    OrderService orderService;
    KafkaTemplate<String, Object> kafkaTemplate;

    public PaymentFailedConsumer(OrderService orderService, KafkaTemplate<String, Object> kafkaTemplate) {
        this.orderService = orderService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = KafkaTopics.PAYMENTS_AUTHORIZED, groupId = "order.service") //todo: make groupids psfs
    public void consume(PaymentFailedEvent paymentFailedEvent) {
        orderService.rejectOrder(
                paymentFailedEvent.orderId(),
                paymentFailedEvent.occurredAt()
        );
        kafkaTemplate.send(
                KafkaTopics.ORDERS_REJECTED,
                paymentFailedEvent.orderId(),
                new OrderConfirmedEvent(
                        UUID.randomUUID(),
                        Instant.now(),
                        paymentFailedEvent.orderId()
                )
        );
    }
}

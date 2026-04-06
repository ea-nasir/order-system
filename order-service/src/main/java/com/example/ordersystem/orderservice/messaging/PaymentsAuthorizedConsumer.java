package com.example.ordersystem.orderservice.messaging;

import com.example.ordersystem.orderservice.logging.LogContext;
import com.example.ordersystem.orderservice.service.OrderService;
import com.example.ordersystem.sharedevents.OrderConfirmedEvent;
import com.example.ordersystem.sharedevents.PaymentAuthorizedEvent;
import config.KafkaTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class PaymentsAuthorizedConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentsAuthorizedConsumer.class);

    private final OrderService orderService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PaymentsAuthorizedConsumer(OrderService orderService, KafkaTemplate<String, Object> kafkaTemplate) {
        this.orderService = orderService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = KafkaTopics.PAYMENTS_AUTHORIZED, groupId = "order.service")
    public void consume(PaymentAuthorizedEvent paymentAuthorizedEvent) {
        LogContext.put(
                paymentAuthorizedEvent.orderId(),
                paymentAuthorizedEvent.eventId().toString(),
                "PaymentAuthorizedEvent"
        );

        try {
            log.info("Consumed payment authorization");

            orderService.confirmOrder(paymentAuthorizedEvent.orderId());
            log.info("Confirmed order after payment authorization");

            OrderConfirmedEvent orderConfirmedEvent = new OrderConfirmedEvent(
                    UUID.randomUUID(),
                    Instant.now(),
                    paymentAuthorizedEvent.orderId()
            );

            kafkaTemplate.send(
                    KafkaTopics.ORDERS_CONFIRMED,
                    paymentAuthorizedEvent.orderId(),
                    orderConfirmedEvent
            );

            log.info("Published event: topic={}, publishedEventType={}, publishedEventId={}",
                    KafkaTopics.ORDERS_CONFIRMED,
                    "OrderConfirmedEvent",
                    orderConfirmedEvent.eventId());
        } finally {
            LogContext.clear();
        }
    }
}
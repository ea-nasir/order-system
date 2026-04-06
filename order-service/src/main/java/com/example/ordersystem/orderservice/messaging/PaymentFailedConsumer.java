package com.example.ordersystem.orderservice.messaging;

import com.example.ordersystem.orderservice.logging.LogContext;
import com.example.ordersystem.orderservice.service.OrderService;
import com.example.ordersystem.sharedevents.OrderRejectedEvent;
import com.example.ordersystem.sharedevents.PaymentFailedEvent;
import config.KafkaTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class PaymentFailedConsumer {
    private static final Logger log = LoggerFactory.getLogger(PaymentFailedConsumer.class);
    OrderService orderService;
    KafkaTemplate<String, Object> kafkaTemplate;

    public PaymentFailedConsumer(OrderService orderService, KafkaTemplate<String, Object> kafkaTemplate) {
        this.orderService = orderService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = KafkaTopics.PAYMENTS_FAILED, groupId = "order.service") //todo: make groupids psfs
    public void consume(PaymentFailedEvent paymentFailedEvent) {
        LogContext.put(
                paymentFailedEvent.orderId(),
                paymentFailedEvent.eventId().toString(),
                PaymentFailedEvent.class.getSimpleName()
        );
        try {
            log.warn("Consumed PaymentFailureEvent with reason={}", paymentFailedEvent.reason());
            orderService.rejectOrder(
                    paymentFailedEvent.orderId()
            );

            log.warn("Rejected order after payment failure");

            OrderRejectedEvent orderRejectedEvent = new OrderRejectedEvent(
                    UUID.randomUUID(),
                    Instant.now(),
                    paymentFailedEvent.orderId(),
                    "Order rejected due to payment failure" //todo: make failure reasons constants
            );

            kafkaTemplate.send(
                    KafkaTopics.ORDERS_REJECTED,
                    paymentFailedEvent.orderId(),
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

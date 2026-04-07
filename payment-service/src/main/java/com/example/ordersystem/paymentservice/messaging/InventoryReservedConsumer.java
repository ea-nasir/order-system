package com.example.ordersystem.paymentservice.messaging;

import com.example.ordersystem.paymentservice.logging.LogContext;
import com.example.ordersystem.paymentservice.model.Payment;
import com.example.ordersystem.paymentservice.model.PaymentAuthorizationResult;
import com.example.ordersystem.paymentservice.service.PaymentService;
import com.example.ordersystem.sharedevents.InventoryReservedEvent;
import com.example.ordersystem.sharedevents.PaymentAuthorizedEvent;
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
public class InventoryReservedConsumer {

    private static final Logger log = LoggerFactory.getLogger(InventoryReservedConsumer.class);

    private final PaymentService paymentService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public InventoryReservedConsumer(PaymentService paymentService, KafkaTemplate<String, Object> kafkaTemplate) {
        this.paymentService = paymentService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = KafkaTopics.INVENTORY_RESERVED, groupId = "payment.service")
    public void consume(InventoryReservedEvent inventoryReservedEvent) {
        LogContext.put(
                inventoryReservedEvent.orderId(),
                inventoryReservedEvent.eventId().toString(),
                "InventoryReservedEvent"
        );

        try {
            String orderId = inventoryReservedEvent.orderId();

            log.info("Consumed inventory reservation: productId={}, quantity={}, totalAmount={}",
                    inventoryReservedEvent.productId(),
                    inventoryReservedEvent.quantity(),
                    inventoryReservedEvent.totalAmount());

            log.info("Starting payment authorization: totalAmount={}",
                    inventoryReservedEvent.totalAmount());

            PaymentAuthorizationResult result = paymentService.authorize(
                    new Payment(orderId, inventoryReservedEvent.totalAmount())
            );

            if (!result.authorized()) {
                PaymentFailedEvent paymentFailedEvent = new PaymentFailedEvent(
                        UUID.randomUUID(),
                        Instant.now(),
                        orderId,
                        result.reason()
                );

                kafkaTemplate.send(KafkaTopics.PAYMENTS_FAILED, orderId, paymentFailedEvent);

                log.warn("Published event: topic={}, publishedEventType={}, publishedEventId={}, failureReason={}",
                        KafkaTopics.PAYMENTS_FAILED,
                        "PaymentFailedEvent",
                        paymentFailedEvent.eventId(),
                        paymentFailedEvent.reason());
                return;
            }

            PaymentAuthorizedEvent paymentAuthorizedEvent = new PaymentAuthorizedEvent(
                    UUID.randomUUID(),
                    Instant.now(),
                    orderId
            );

            kafkaTemplate.send(KafkaTopics.PAYMENTS_AUTHORIZED, orderId, paymentAuthorizedEvent);

            log.info("Published event: topic={}, publishedEventType={}, publishedEventId={}",
                    KafkaTopics.PAYMENTS_AUTHORIZED,
                    "PaymentAuthorizedEvent",
                    paymentAuthorizedEvent.eventId());
        } finally {
            LogContext.clear();
        }
    }
}
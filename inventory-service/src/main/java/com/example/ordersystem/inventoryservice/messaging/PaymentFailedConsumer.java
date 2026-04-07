package com.example.ordersystem.inventoryservice.messaging;

import com.example.ordersystem.inventoryservice.logging.LogContext;
import com.example.ordersystem.inventoryservice.service.InventoryService;
import com.example.ordersystem.sharedevents.PaymentFailedEvent;
import config.KafkaTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentFailedConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentFailedConsumer.class);

    private final InventoryService inventoryService;

    public PaymentFailedConsumer(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @KafkaListener(topics = KafkaTopics.PAYMENTS_FAILED, groupId = "inventory.service")
    public void consume(PaymentFailedEvent paymentFailedEvent) {
        LogContext.put(
                paymentFailedEvent.orderId(),
                paymentFailedEvent.eventId().toString(),
                "PaymentFailedEvent"
        );

        try {
            log.warn("Consumed payment failure for compensation: reason={}",
                    paymentFailedEvent.reason());

            inventoryService.releaseReservation(paymentFailedEvent.orderId());

            log.info("Completed compensation: action=releaseReservation");
        } finally {
            LogContext.clear();
        }
    }
}
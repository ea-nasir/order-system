package com.example.ordersystem.inventoryservice.messaging;

import com.example.ordersystem.inventoryservice.service.InventoryService;
import com.example.ordersystem.sharedevents.OrderCreatedEvent;
import com.example.ordersystem.sharedevents.PaymentFailedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class PaymentFailedConsumerIntegrationTest {

    @Autowired
    private OrderCreatedConsumer orderCreatedConsumer;

    @Autowired
    private PaymentFailedConsumer paymentFailedConsumer;

    @Autowired
    private InventoryService inventoryService;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void consume_shouldReleaseReservationAfterPaymentFailure() {
        OrderCreatedEvent createEvent = new OrderCreatedEvent(
                UUID.randomUUID(),
                Instant.now(),
                "order-1",
                "customer-1",
                "CHAIR",
                2,
                new BigDecimal("100.00"),
                new BigDecimal("200.00")
        );

        int stockBeforeReservation = inventoryService.getItemFromStock("CHAIR");

        orderCreatedConsumer.consume(createEvent);

        int stockAfterReservation = inventoryService.getItemFromStock("CHAIR");
        assertEquals(stockBeforeReservation - 2, stockAfterReservation);

        PaymentFailedEvent paymentFailedEvent = new PaymentFailedEvent(
                UUID.randomUUID(),
                Instant.now(),
                "order-1",
                "PayPal declined"
        );

        paymentFailedConsumer.consume(paymentFailedEvent);

        int stockAfterRelease = inventoryService.getItemFromStock("CHAIR");
        assertEquals(stockBeforeReservation, stockAfterRelease);
    }
}
package com.example.ordersystem.inventoryservice.messaging;

import com.example.ordersystem.inventoryservice.service.InventoryService;
import com.example.ordersystem.sharedevents.InventoryRejectedEvent;
import com.example.ordersystem.sharedevents.InventoryReservedEvent;
import com.example.ordersystem.sharedevents.OrderCreatedEvent;
import config.KafkaTopics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@SpringBootTest
class OrderCreatedConsumerIntegrationTest {

    @Autowired
    private OrderCreatedConsumer orderCreatedConsumer;

    @Autowired
    private InventoryService inventoryService;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @BeforeEach
    void resetStockIfNeeded() {
        // Fresh Spring test context should usually recreate the service,
        // so no explicit reset needed for now.
        // Leave this hook here in case you later make the service state longer-lived.
    }

    @Test
    void consume_shouldReserveInventoryAndPublishInventoryReservedEvent() {
        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID(),
                Instant.now(),
                "order-1",
                "customer-1",
                "CHAIR",
                2,
                new BigDecimal("100.00"),
                new BigDecimal("200.00")
        );

        int stockBefore = inventoryService.getItemFromStock("CHAIR");

        orderCreatedConsumer.consume(event);

        int stockAfter = inventoryService.getItemFromStock("CHAIR");
        assertEquals(stockBefore - 2, stockAfter);

        ArgumentCaptor<InventoryReservedEvent> eventCaptor =
                ArgumentCaptor.forClass(InventoryReservedEvent.class);

        verify(kafkaTemplate).send(
                eq(KafkaTopics.INVENTORY_RESERVED),
                eq("order-1"),
                eventCaptor.capture()
        );

        InventoryReservedEvent publishedEvent = eventCaptor.getValue();
        assertNotNull(publishedEvent.eventId());
        assertNotNull(publishedEvent.occurredAt());
        assertEquals("order-1", publishedEvent.orderId());
        assertEquals("CHAIR", publishedEvent.productId());
        assertEquals(2, publishedEvent.quantity());
        assertEquals(new BigDecimal("200.00"), publishedEvent.totalAmount());
    }

    @Test
    void consume_shouldPublishInventoryRejectedEventWhenStockIsInsufficient() {
        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID(),
                Instant.now(),
                "order-2",
                "customer-1",
                "CHAIR",
                999,
                new BigDecimal("100.00"),
                new BigDecimal("99900.00")
        );

        int stockBefore = inventoryService.getItemFromStock("CHAIR");

        orderCreatedConsumer.consume(event);

        int stockAfter = inventoryService.getItemFromStock("CHAIR");
        assertEquals(stockBefore, stockAfter);

        ArgumentCaptor<InventoryRejectedEvent> eventCaptor =
                ArgumentCaptor.forClass(InventoryRejectedEvent.class);

        verify(kafkaTemplate).send(
                eq(KafkaTopics.INVENTORY_REJECTED),
                eq("order-2"),
                eventCaptor.capture()
        );

        InventoryRejectedEvent publishedEvent = eventCaptor.getValue();
        assertNotNull(publishedEvent.eventId());
        assertNotNull(publishedEvent.occurredAt());
        assertEquals("order-2", publishedEvent.orderId());
        assertNotNull(publishedEvent.reason());
    }
}
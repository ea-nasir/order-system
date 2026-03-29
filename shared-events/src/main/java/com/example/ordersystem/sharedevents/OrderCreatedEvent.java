package com.example.ordersystem.sharedevents;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID eventId,
        Instant occurredAt,
        String orderId,
        String customerId,
        String productId,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal totalAmount
) {
}
package com.example.ordersystem.sharedevents;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID eventId,
        Instant occurredAt,
        String orderId,
        String customerId,
        BigDecimal totalAmount
) {
}
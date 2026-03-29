package com.example.ordersystem.sharedevents;

import java.time.Instant;
import java.util.UUID;

public record PaymentFailedEvent(
        UUID eventId,
        Instant occurredAt,
        String orderId,
        String reason
) {
}
package com.example.ordersystem.sharedevents;

import java.time.Instant;
import java.util.UUID;

public record OrderConfirmedEvent(
        UUID eventId,
        Instant occurredAt,
        String orderId
) {
}
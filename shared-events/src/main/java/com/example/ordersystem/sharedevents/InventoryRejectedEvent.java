package com.example.ordersystem.sharedevents;

import java.time.Instant;
import java.util.UUID;

public record InventoryRejectedEvent(
        UUID eventId,
        Instant occurredAt,
        String orderId,
        String reason
) {
}
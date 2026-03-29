package com.example.ordersystem.sharedevents;

import java.time.Instant;
import java.util.UUID;

public record InventoryReservedEvent(
        UUID eventId,
        Instant occurredAt,
        String orderId,
        String productId,
        int quantity
) {
}
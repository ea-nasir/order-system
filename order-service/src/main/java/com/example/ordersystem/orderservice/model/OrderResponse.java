package com.example.ordersystem.orderservice.model;

import com.example.ordersystem.orderservice.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderResponse(
        String orderId,
        String customerId,
        String productId,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal totalAmount,
        OrderStatus status,
        Instant createdAt
) {
}

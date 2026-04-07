package com.example.ordersystem.paymentservice.model;

import java.math.BigDecimal;

public record PaypalAuthorizeRequest(
        String orderId,
        BigDecimal amount
) {
}
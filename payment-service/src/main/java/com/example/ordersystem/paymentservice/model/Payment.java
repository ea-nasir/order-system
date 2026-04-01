package com.example.ordersystem.paymentservice.model;

import java.math.BigDecimal;

public record Payment(
        String orderId,
        BigDecimal amount
) {

}

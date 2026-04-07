package com.example.ordersystem.paymentservice.model;

public record PaymentAuthorizationResult(
        boolean authorized,
        String reason
) {
}
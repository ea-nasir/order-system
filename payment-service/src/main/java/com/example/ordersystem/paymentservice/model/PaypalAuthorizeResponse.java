package com.example.ordersystem.paymentservice.model;

public record PaypalAuthorizeResponse(
        boolean authorized,
        String reason
) {
}
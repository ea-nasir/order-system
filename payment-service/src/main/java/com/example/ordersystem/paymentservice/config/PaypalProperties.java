package com.example.ordersystem.paymentservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "paypal")
public record PaypalProperties(String baseUrl) {
}
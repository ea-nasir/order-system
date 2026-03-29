package com.example.ordersystem.orderservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicsConfig {

    @Bean
    public NewTopic ordersCreatedTopic() {
        return new NewTopic("orders.created", 1, (short) 1);
    }

    @Bean
    public NewTopic inventoryReservedTopic() {
        return new NewTopic("inventory.reserved", 1, (short) 1);
    }

    @Bean
    public NewTopic inventoryRejectedTopic() {
        return new NewTopic("inventory.rejected", 1, (short) 1);
    }

    @Bean
    public NewTopic paymentsAuthorizedTopic() {
        return new NewTopic("payments.authorized", 1, (short) 1);
    }

    @Bean
    public NewTopic paymentsFailedTopic() {
        return new NewTopic("payments.failed", 1, (short) 1);
    }

    @Bean
    public NewTopic ordersConfirmedTopic() {
        return new NewTopic("orders.confirmed", 1, (short) 1);
    }

    @Bean
    public NewTopic ordersRejectedTopic() {
        return new NewTopic("orders.rejected", 1, (short) 1);
    }
}
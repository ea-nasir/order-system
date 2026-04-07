package com.example.ordersystem.paymentservice.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(PaypalProperties.class)
public class PaypalClientConfig {

    @Bean
    public RestClient paypalRestClient(RestClient.Builder builder, PaypalProperties paypalProperties) {
        return builder
                .baseUrl(paypalProperties.baseUrl())
                .build();
    }
}
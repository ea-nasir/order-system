package com.example.ordersystem.paymentservice.client;

import com.example.ordersystem.paymentservice.model.PaypalAuthorizeRequest;
import com.example.ordersystem.paymentservice.model.PaypalAuthorizeResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class PaypalClient {

    private final RestClient paypalRestClient;

    public PaypalClient(RestClient paypalRestClient) {
        this.paypalRestClient = paypalRestClient;
    }

    public PaypalAuthorizeResponse authorize(PaypalAuthorizeRequest request) {
        return paypalRestClient.post()
                .uri("/paypal/authorize")
                .body(request)
                .retrieve()
                .body(PaypalAuthorizeResponse.class);
    }
}
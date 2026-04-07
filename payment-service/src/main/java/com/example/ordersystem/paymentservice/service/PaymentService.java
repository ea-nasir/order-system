package com.example.ordersystem.paymentservice.service;

import com.example.ordersystem.paymentservice.client.PaypalClient;
import com.example.ordersystem.paymentservice.model.Payment;
import com.example.ordersystem.paymentservice.model.PaymentAuthorizationResult;
import com.example.ordersystem.paymentservice.model.PaypalAuthorizeRequest;
import com.example.ordersystem.paymentservice.model.PaypalAuthorizeResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

@Service
public class PaymentService {

    private final PaypalClient paypalClient;

    public PaymentService(PaypalClient paypalClient) {
        this.paypalClient = paypalClient;
    }

    public PaymentAuthorizationResult authorize(Payment payment) {
        try {
            PaypalAuthorizeResponse response = paypalClient.authorize(
                    new PaypalAuthorizeRequest(payment.orderId(), payment.amount())
            );

            return new PaymentAuthorizationResult(
                    response.authorized(),
                    response.reason()
            );
        } catch (RestClientException ex) {
            return new PaymentAuthorizationResult(
                    false,
                    "PayPal unavailable"
            );
        }
    }
}
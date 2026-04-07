package com.example.ordersystem.paymentservice.service;

import com.example.ordersystem.paymentservice.client.PaypalClient;
import com.example.ordersystem.paymentservice.model.Payment;
import com.example.ordersystem.paymentservice.model.PaypalAuthorizeRequest;
import com.example.ordersystem.paymentservice.model.PaypalAuthorizeResponse;
import com.example.ordersystem.paymentservice.model.PaymentAuthorizationResult;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentServiceTest {

    private final PaypalClient paypalClient = mock(PaypalClient.class);
    private final PaymentService paymentService = new PaymentService(paypalClient);

    @Test
    void authorize_shouldApproveWhenPaypalApproves() {
        when(paypalClient.authorize(any(PaypalAuthorizeRequest.class)))
                .thenReturn(new PaypalAuthorizeResponse(true, "Approved"));

        PaymentAuthorizationResult result = paymentService.authorize(
                new Payment("order-1", BigDecimal.valueOf(100))
        );

        assertTrue(result.authorized());
        assertEquals("Approved", result.reason());
    }

    @Test
    void authorize_shouldDeclineWhenPaypalDeclines() {
        when(paypalClient.authorize(any(PaypalAuthorizeRequest.class)))
                .thenReturn(new PaypalAuthorizeResponse(false, "Insufficient funds"));

        PaymentAuthorizationResult result = paymentService.authorize(
                new Payment("order-1", BigDecimal.valueOf(1500))
        );

        assertFalse(result.authorized());
        assertEquals("Insufficient funds", result.reason());
    }

    @Test
    void authorize_shouldReturnUnavailableWhenPaypalThrows() {
        when(paypalClient.authorize(any(PaypalAuthorizeRequest.class)))
                .thenThrow(new RestClientException("Connection refused"));

        PaymentAuthorizationResult result = paymentService.authorize(
                new Payment("order-1", BigDecimal.valueOf(1500))
        );

        assertFalse(result.authorized());
        assertEquals("PayPal unavailable", result.reason());
    }
}
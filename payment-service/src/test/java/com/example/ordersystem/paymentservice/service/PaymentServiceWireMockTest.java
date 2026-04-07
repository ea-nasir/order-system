package com.example.ordersystem.paymentservice.service;

import com.example.ordersystem.paymentservice.config.PaypalClientConfig;
import com.example.ordersystem.paymentservice.config.PaypalProperties;
import com.example.ordersystem.paymentservice.model.Payment;
import com.example.ordersystem.paymentservice.model.PaymentAuthorizationResult;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;

class PaymentServiceWireMockTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Test
    void authorize_shouldApproveWhenPaypalReturnsApproved() {
        wireMock.stubFor(post(urlEqualTo("/paypal/authorize"))
                .willReturn(okJson("""
                    {
                      "authorized": true,
                      "reason": "Approved"
                    }
                """)));

        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withBean(PaypalProperties.class, () -> new PaypalProperties(wireMock.getRuntimeInfo().getHttpBaseUrl()))
                .withBean(RestClient.Builder.class, RestClient::builder)
                .withUserConfiguration(PaypalClientConfig.class)
                .withBean(com.example.ordersystem.paymentservice.client.PaypalClient.class)
                .withBean(PaymentService.class);

        runner.run(context -> {
            PaymentService paymentService = context.getBean(PaymentService.class);

            PaymentAuthorizationResult result = paymentService.authorize(
                    new Payment("order-1", BigDecimal.valueOf(100))
            );

            assertTrue(result.authorized());
            assertEquals("Approved", result.reason());
        });
    }

    @Test
    void authorize_shouldDeclineWhenPaypalReturnsDeclined() {
        wireMock.stubFor(post(urlEqualTo("/paypal/authorize"))
                .willReturn(okJson("""
                    {
                      "authorized": false,
                      "reason": "Insufficient funds"
                    }
                """)));

        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withBean(PaypalProperties.class, () -> new PaypalProperties(wireMock.getRuntimeInfo().getHttpBaseUrl()))
                .withBean(RestClient.Builder.class, RestClient::builder)
                .withUserConfiguration(PaypalClientConfig.class)
                .withBean(com.example.ordersystem.paymentservice.client.PaypalClient.class)
                .withBean(PaymentService.class);

        runner.run(context -> {
            PaymentService paymentService = context.getBean(PaymentService.class);

            PaymentAuthorizationResult result = paymentService.authorize(
                    new Payment("order-1", BigDecimal.valueOf(1500))
            );

            assertFalse(result.authorized());
            assertEquals("Insufficient funds", result.reason());
        });
    }

    @Test
    void authorize_shouldReturnUnavailableWhenPaypalErrors() {
        wireMock.stubFor(post(urlEqualTo("/paypal/authorize"))
                .willReturn(serverError()));

        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withBean(PaypalProperties.class, () -> new PaypalProperties(wireMock.getRuntimeInfo().getHttpBaseUrl()))
                .withBean(RestClient.Builder.class, RestClient::builder)
                .withUserConfiguration(PaypalClientConfig.class)
                .withBean(com.example.ordersystem.paymentservice.client.PaypalClient.class)
                .withBean(PaymentService.class);

        runner.run(context -> {
            PaymentService paymentService = context.getBean(PaymentService.class);

            PaymentAuthorizationResult result = paymentService.authorize(
                    new Payment("order-1", BigDecimal.valueOf(1500))
            );

            assertFalse(result.authorized());
            assertEquals("PayPal unavailable", result.reason());
        });
    }
}
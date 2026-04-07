package com.example.ordersystem.paymentservice.service;

import com.example.ordersystem.paymentservice.client.PaypalClient;
import com.example.ordersystem.paymentservice.model.PaypalAuthorizeRequest;
import com.example.ordersystem.paymentservice.model.PaypalAuthorizeResponse;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;

class PaymentServiceWireMockTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig()
                    .dynamicPort()
                    .http2PlainDisabled(true))
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

        RestClient restClient = RestClient.builder()
                .requestFactory(new SimpleClientHttpRequestFactory())
                .baseUrl(wireMock.getRuntimeInfo().getHttpBaseUrl())
                .build();

        PaypalClient paypalClient = new PaypalClient(restClient);

        PaypalAuthorizeResponse response = paypalClient.authorize(
                new PaypalAuthorizeRequest("order-1", BigDecimal.valueOf(999))
        );

        wireMock.verify(postRequestedFor(urlEqualTo("/paypal/authorize")));
        assertNotNull(response);
        assertTrue(response.authorized());
        assertEquals("Approved", response.reason());
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

        RestClient restClient = RestClient.builder()
                .requestFactory(new SimpleClientHttpRequestFactory())
                .baseUrl(wireMock.getRuntimeInfo().getHttpBaseUrl())
                .build();

        PaypalClient paypalClient = new PaypalClient(restClient);
        PaypalAuthorizeResponse response = paypalClient.authorize(
                new PaypalAuthorizeRequest("order-1", BigDecimal.valueOf(1500))
        );

        wireMock.verify(postRequestedFor(urlEqualTo("/paypal/authorize")));
        assertNotNull(response);
        assertFalse(response.authorized());
        assertEquals("Insufficient funds", response.reason());
    }

    @Test
    void authorize_shouldThrowWhenPaypalErrors() {
        wireMock.stubFor(post(urlEqualTo("/paypal/authorize"))
                .willReturn(serverError()));

        RestClient restClient = RestClient.builder()
                .requestFactory(new SimpleClientHttpRequestFactory())
                .baseUrl(wireMock.getRuntimeInfo().getHttpBaseUrl())
                .build();

        PaypalClient paypalClient = new PaypalClient(restClient);

        assertThrows(Exception.class, () -> paypalClient.authorize(
                new PaypalAuthorizeRequest("order-1", BigDecimal.valueOf(999))
        ));

        wireMock.verify(postRequestedFor(urlEqualTo("/paypal/authorize")));
    }
}
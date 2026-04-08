package com.example.ordersystem.paymentservice.messaging;

import com.example.ordersystem.paymentservice.client.PaypalClient;
import com.example.ordersystem.sharedevents.InventoryReservedEvent;
import com.example.ordersystem.paymentservice.model.PaypalAuthorizeResponse;
import com.example.ordersystem.paymentservice.service.PaymentService;
import com.example.ordersystem.sharedevents.PaymentAuthorizedEvent;
import com.example.ordersystem.sharedevents.PaymentFailedEvent;
import config.KafkaTopics;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class InventoryReservedConsumerIntegrationTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig()
                    .dynamicPort()
                    .http2PlainDisabled(true))
            .build();

    private KafkaTemplate<String, Object> kafkaTemplate;
    private InventoryReservedConsumer inventoryReservedConsumer;

    @BeforeEach
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);

        RestClient restClient = RestClient.builder()
                .requestFactory(new SimpleClientHttpRequestFactory())
                .baseUrl(wireMock.getRuntimeInfo().getHttpBaseUrl())
                .build();

        PaypalClient paypalClient = new PaypalClient(restClient);
        PaymentService paymentService = new PaymentService(paypalClient);

        inventoryReservedConsumer = new InventoryReservedConsumer(paymentService, kafkaTemplate);
    }

    @Test
    void consume_shouldPublishPaymentAuthorizedEventWhenPaypalApproves() {
        wireMock.stubFor(post(urlEqualTo("/paypal/authorize"))
                .willReturn(okJson("""
                    {
                      "authorized": true,
                      "reason": "Approved"
                    }
                """)));

        InventoryReservedEvent event =
                new InventoryReservedEvent(
                        UUID.randomUUID(),
                        Instant.now(),
                        "order-1",
                        "CHAIR",
                        2,
                        new BigDecimal("200.00")
                );

        inventoryReservedConsumer.consume(event);

        ArgumentCaptor<PaymentAuthorizedEvent> eventCaptor =
                ArgumentCaptor.forClass(PaymentAuthorizedEvent.class);

        verify(kafkaTemplate).send(
                eq(KafkaTopics.PAYMENTS_AUTHORIZED),
                eq("order-1"),
                eventCaptor.capture()
        );

        PaymentAuthorizedEvent publishedEvent = eventCaptor.getValue();
        assertNotNull(publishedEvent.eventId());
        assertNotNull(publishedEvent.occurredAt());
        assertEquals("order-1", publishedEvent.orderId());

        wireMock.verify(postRequestedFor(urlEqualTo("/paypal/authorize")));
    }

    @Test
    void consume_shouldPublishPaymentFailedEventWhenPaypalDeclines() {
        wireMock.stubFor(post(urlEqualTo("/paypal/authorize"))
                .willReturn(okJson("""
                    {
                      "authorized": false,
                      "reason": "Insufficient funds"
                    }
                """)));

        InventoryReservedEvent event =
                new com.example.ordersystem.sharedevents.InventoryReservedEvent(
                        UUID.randomUUID(),
                        Instant.now(),
                        "order-2",
                        "CHAIR",
                        2,
                        new BigDecimal("1500.00")
                );

        inventoryReservedConsumer.consume(event);

        ArgumentCaptor<PaymentFailedEvent> eventCaptor =
                ArgumentCaptor.forClass(PaymentFailedEvent.class);

        verify(kafkaTemplate).send(
                eq(KafkaTopics.PAYMENTS_FAILED),
                eq("order-2"),
                eventCaptor.capture()
        );

        PaymentFailedEvent publishedEvent = eventCaptor.getValue();
        assertNotNull(publishedEvent.eventId());
        assertNotNull(publishedEvent.occurredAt());
        assertEquals("order-2", publishedEvent.orderId());
        assertEquals("Insufficient funds", publishedEvent.reason());

        wireMock.verify(postRequestedFor(urlEqualTo("/paypal/authorize")));
    }

    @Test
    void consume_shouldPublishPaymentFailedEventWhenPaypalErrors() {
        wireMock.stubFor(post(urlEqualTo("/paypal/authorize"))
                .willReturn(serverError()));

        InventoryReservedEvent event =
                new com.example.ordersystem.sharedevents.InventoryReservedEvent(
                        UUID.randomUUID(),
                        Instant.now(),
                        "order-3",
                        "CHAIR",
                        2,
                        new BigDecimal("1500.00")
                );

        inventoryReservedConsumer.consume(event);

        ArgumentCaptor<PaymentFailedEvent> eventCaptor =
                ArgumentCaptor.forClass(PaymentFailedEvent.class);

        verify(kafkaTemplate).send(
                eq(KafkaTopics.PAYMENTS_FAILED),
                eq("order-3"),
                eventCaptor.capture()
        );

        PaymentFailedEvent publishedEvent = eventCaptor.getValue();
        assertNotNull(publishedEvent.eventId());
        assertNotNull(publishedEvent.occurredAt());
        assertEquals("order-3", publishedEvent.orderId());
        assertEquals("PayPal unavailable", publishedEvent.reason());

        wireMock.verify(postRequestedFor(urlEqualTo("/paypal/authorize")));
    }
}
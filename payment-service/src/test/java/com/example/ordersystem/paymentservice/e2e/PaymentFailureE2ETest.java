package com.example.ordersystem.paymentservice.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;

class PaymentFailureE2ETest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig()
                    .port(8089)
                    .http2PlainDisabled(true))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void paymentFailureFlow_shouldEndWithRejectedOrder() throws Exception {
        wireMock.stubFor(post(urlEqualTo("/paypal/authorize"))
                .willReturn(okJson("""
                            {
                              "authorized": false,
                              "reason": "Insufficient funds"
                            }
                        """)));

        RestClient restClient = RestClient.builder()
                .requestFactory(new SimpleClientHttpRequestFactory())
                .baseUrl("http://localhost:8080")
                .build();

        // Create order
        String createResponseBody = restClient.post()
                .uri("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                            {
                              "customerId": "customer-1",
                              "productId": "CHAIR",
                              "quantity": 2,
                              "unitPrice": 500.00
                            }
                        """)
                .retrieve()
                .body(String.class);

        assertNotNull(createResponseBody);

        JsonNode createdOrderJson = objectMapper.readTree(createResponseBody);
        String orderId = createdOrderJson.get("orderId").asText();

        assertNotNull(orderId);
        assertFalse(orderId.isBlank());

        // Poll until the async flow finishes
        String finalStatus = pollOrderStatus(restClient, orderId, Duration.ofSeconds(10), Duration.ofMillis(1000));

        assertEquals("REJECTED", finalStatus);

        wireMock.verify(postRequestedFor(urlEqualTo("/paypal/authorize")));
    }

    private String pollOrderStatus(RestClient restClient,
                                   String orderId,
                                   Duration timeout,
                                   Duration interval) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        String latestStatus = null;

        while (System.nanoTime() < deadline) {
            String getResponseBody = restClient.get()
                    .uri("/orders/{orderId}", orderId)
                    .retrieve()
                    .body(String.class);

            assertNotNull(getResponseBody);

            JsonNode orderJson = objectMapper.readTree(getResponseBody);
            latestStatus = orderJson.get("status").asText();

            if ("REJECTED".equals(latestStatus) || "CONFIRMED".equals(latestStatus)) {
                return latestStatus;
            }

            Thread.sleep(interval.toMillis());
        }

        fail("Timed out waiting for final order status. Latest status was: " + latestStatus);
        return latestStatus;
    }
}
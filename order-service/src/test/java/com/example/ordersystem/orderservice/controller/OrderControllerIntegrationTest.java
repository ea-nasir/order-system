package com.example.ordersystem.orderservice.controller;

import com.example.ordersystem.orderservice.model.OrderResponse;
import com.example.ordersystem.orderservice.repository.OrderRepository;
import com.example.ordersystem.sharedevents.OrderCreatedEvent;
import config.KafkaTopics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    OrderRepository orderRepository;

    @MockBean
    KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void postOrders_shouldPersistAndPublish() throws Exception {
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "customerId": "customer-1",
                              "productId": "CHAIR",
                              "quantity": 2,
                              "unitPrice": 100.00
                            }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.productId").value("CHAIR"))
                .andExpect(jsonPath("$.quantity").value(2));

        assertEquals(1, orderRepository.count());
        verify(kafkaTemplate).send(eq(KafkaTopics.ORDERS_CREATED), anyString(), any(OrderCreatedEvent.class));
    }
    @Test
    void firstPostOrderAndThenGetOrder_shouldGetOrder() throws Exception {
        String expectedCustomerId = "customer-1";
        String expectedProductId = "CHAIR";
        int expectedQuantity = 2;
        BigDecimal expectedUnitPrice = new BigDecimal("100.00");

        String postResponse = mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "customerId": "customer-1",
                          "productId": "CHAIR",
                          "quantity": 2,
                          "unitPrice": 100.00
                        }
                    """))
                .andReturn()
                .getResponse()
                .getContentAsString();

        OrderResponse createdOrder = objectMapper.readValue(postResponse, OrderResponse.class);
        String orderId = createdOrder.orderId();

        mockMvc.perform(get("/orders/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.customerId").value(expectedCustomerId))
                .andExpect(jsonPath("$.productId").value(expectedProductId))
                .andExpect(jsonPath("$.quantity").value(expectedQuantity))
                .andExpect(jsonPath("$.unitPrice").value(expectedUnitPrice.doubleValue()));
    }
}
package com.example.ordersystem.orderservice.service;

        import com.example.ordersystem.orderservice.entity.OrderEntity;
        import com.example.ordersystem.orderservice.entity.OrderStatus;
        import com.example.ordersystem.orderservice.model.CreateOrderRequest;
        import com.example.ordersystem.orderservice.model.OrderResponse;
        import com.example.ordersystem.orderservice.repository.OrderRepository;
        import com.example.ordersystem.sharedevents.OrderCreatedEvent;
        import config.KafkaTopics;
        import org.junit.jupiter.api.BeforeEach;
        import org.junit.jupiter.api.Test;
        import org.mockito.ArgumentCaptor;
        import org.springframework.kafka.core.KafkaTemplate;

        import java.math.BigDecimal;
        import java.time.Instant;
        import java.util.Optional;

        import static org.junit.jupiter.api.Assertions.*;
        import static org.mockito.ArgumentMatchers.eq;
        import static org.mockito.Mockito.*;

class OrderServiceTest {

    private OrderRepository orderRepository;
    private KafkaTemplate kafkaTemplate;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        kafkaTemplate = mock(KafkaTemplate.class);
        orderService = new OrderService(orderRepository, kafkaTemplate);
    }

    @Test
    void createOrder_shouldSaveOrderPublishEventAndReturnResponse() {
        CreateOrderRequest request = new CreateOrderRequest(
                "customer-1",
                "CHAIR",
                2,
                new BigDecimal("99.99")
        );

        OrderResponse response = orderService.createOrder(request);

        ArgumentCaptor<OrderEntity> entityCaptor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository).save(entityCaptor.capture());

        OrderEntity savedEntity = entityCaptor.getValue();
        assertNotNull(savedEntity.getOrderId());
        assertEquals("customer-1", savedEntity.getCustomerId());
        assertEquals("CHAIR", savedEntity.getProductId());
        assertEquals(2, savedEntity.getQuantity());
        assertEquals(new BigDecimal("99.99"), savedEntity.getUnitPrice());
        assertEquals(new BigDecimal("199.98"), savedEntity.getTotalAmount());
        assertEquals(OrderStatus.CREATED, savedEntity.getStatus());
        assertNotNull(savedEntity.getCreatedAt());

        ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(kafkaTemplate).send(
                eq(KafkaTopics.ORDERS_CREATED),
                eq(savedEntity.getOrderId()),
                eventCaptor.capture()
        );

        OrderCreatedEvent publishedEvent = eventCaptor.getValue();
        assertNotNull(publishedEvent.eventId());
        assertNotNull(publishedEvent.occurredAt());
        assertEquals(savedEntity.getOrderId(), publishedEvent.orderId());
        assertEquals(savedEntity.getCustomerId(), publishedEvent.customerId());
        assertEquals(savedEntity.getProductId(), publishedEvent.productId());
        assertEquals(savedEntity.getQuantity(), publishedEvent.quantity());
        assertEquals(savedEntity.getUnitPrice(), publishedEvent.unitPrice());
        assertEquals(savedEntity.getTotalAmount(), publishedEvent.totalAmount());

        assertEquals(savedEntity.getOrderId(), response.orderId());
        assertEquals(savedEntity.getCustomerId(), response.customerId());
        assertEquals(savedEntity.getProductId(), response.productId());
        assertEquals(savedEntity.getQuantity(), response.quantity());
        assertEquals(savedEntity.getUnitPrice(), response.unitPrice());
        assertEquals(savedEntity.getTotalAmount(), response.totalAmount());
        assertEquals(OrderStatus.CREATED, response.status());
        assertNotNull(response.createdAt());
    }

    @Test
    void getOrder_shouldReturnMappedResponse() {
        Instant createdAt = Instant.parse("2026-04-06T12:00:00Z");
        OrderEntity entity = new OrderEntity(
                "order-1",
                "customer-1",
                "CHAIR",
                2,
                new BigDecimal("99.99"),
                new BigDecimal("199.98"),
                OrderStatus.CREATED,
                createdAt
        );

        when(orderRepository.findById("order-1")).thenReturn(Optional.of(entity));

        OrderResponse response = orderService.getOrder("order-1");

        assertEquals("order-1", response.orderId());
        assertEquals("customer-1", response.customerId());
        assertEquals("CHAIR", response.productId());
        assertEquals(2, response.quantity());
        assertEquals(new BigDecimal("99.99"), response.unitPrice());
        assertEquals(new BigDecimal("199.98"), response.totalAmount());
        assertEquals(OrderStatus.CREATED, response.status());
        assertNotNull(response.createdAt());
    }

    @Test
    void rejectOrder_shouldSetStatusToRejectedAndSave() {
        OrderEntity entity = new OrderEntity(
                "order-1",
                "customer-1",
                "CHAIR",
                2,
                new BigDecimal("99.99"),
                new BigDecimal("199.98"),
                OrderStatus.CREATED,
                Instant.now()
        );

        when(orderRepository.findById("order-1")).thenReturn(Optional.of(entity));

        orderService.rejectOrder("order-1");

        assertEquals(OrderStatus.REJECTED, entity.getStatus());
        verify(orderRepository).save(entity);
    }

    @Test
    void confirmOrder_shouldSetStatusToConfirmedAndSave() {
        OrderEntity entity = new OrderEntity(
                "order-1",
                "customer-1",
                "CHAIR",
                2,
                new BigDecimal("99.99"),
                new BigDecimal("199.98"),
                OrderStatus.CREATED,
                Instant.now()
        );

        when(orderRepository.findById("order-1")).thenReturn(Optional.of(entity));

        orderService.confirmOrder("order-1");

        assertEquals(OrderStatus.CONFIRMED, entity.getStatus());
        verify(orderRepository).save(entity);
    }
}
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
    private KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;
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

        BigDecimal expectedTotalAmount = new BigDecimal("199.98");

        OrderResponse response = orderService.createOrder(request);

        ArgumentCaptor<OrderEntity> entityCaptor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository).save(entityCaptor.capture());

        OrderEntity savedEntity = entityCaptor.getValue();

        assertNotNull(savedEntity.getOrderId());
        assertEquals(request.customerId(), savedEntity.getCustomerId());
        assertEquals(request.productId(), savedEntity.getProductId());
        assertEquals(request.quantity(), savedEntity.getQuantity());
        assertEquals(request.unitPrice(), savedEntity.getUnitPrice());
        assertEquals(expectedTotalAmount, savedEntity.getTotalAmount());
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
        assertEquals(request.customerId(), publishedEvent.customerId());
        assertEquals(request.productId(), publishedEvent.productId());
        assertEquals(request.quantity(), publishedEvent.quantity());
        assertEquals(request.unitPrice(), publishedEvent.unitPrice());
        assertEquals(expectedTotalAmount, publishedEvent.totalAmount());

        assertEquals(savedEntity.getOrderId(), response.orderId());
        assertEquals(request.customerId(), response.customerId());
        assertEquals(request.productId(), response.productId());
        assertEquals(request.quantity(), response.quantity());
        assertEquals(request.unitPrice(), response.unitPrice());
        assertEquals(expectedTotalAmount, response.totalAmount());
        assertEquals(OrderStatus.CREATED, response.status());
        assertEquals(savedEntity.getCreatedAt(), response.createdAt());
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
        assertEquals(createdAt, response.createdAt());
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
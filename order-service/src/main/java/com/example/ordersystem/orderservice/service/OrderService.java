package com.example.ordersystem.orderservice.service;

import com.example.ordersystem.orderservice.entity.OrderEntity;
import com.example.ordersystem.orderservice.entity.OrderStatus;
import com.example.ordersystem.orderservice.model.CreateOrderRequest;
import com.example.ordersystem.orderservice.model.OrderResponse;
import com.example.ordersystem.orderservice.repository.OrderRepository;
import com.example.ordersystem.sharedevents.OrderCreatedEvent;
import config.KafkaTopics;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;
    public OrderService(OrderRepository orderRepository,
                        KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate){
        this.orderRepository = orderRepository;
        this.kafkaTemplate = kafkaTemplate;
    }
    public OrderResponse createOrder(CreateOrderRequest request){
        String orderId = UUID.randomUUID().toString();
        BigDecimal totalAmount = request.unitPrice().multiply(BigDecimal.valueOf(request.quantity()));

        OrderEntity orderEntity = new OrderEntity(
                orderId,
                request.customerId(),
                request.productId(),
                request.quantity(),
                request.unitPrice(),
                totalAmount,
                OrderStatus.CREATED,
                Instant.now()
        );

        orderRepository.save(orderEntity);

        OrderCreatedEvent orderCreatedEvent = new OrderCreatedEvent(
                UUID.randomUUID(),
                Instant.now(),
                orderId,
                request.customerId(),
                request.productId(),
                request.quantity(),
                request.unitPrice(),
                totalAmount
        );

        kafkaTemplate.send(KafkaTopics.ORDERS_CREATED,orderId,orderCreatedEvent);

        return toResponse(orderEntity);
    }

    public OrderResponse getOrder(String orderId) {
        OrderEntity entity = orderRepository.findById(orderId)
                .orElseThrow();

        return toResponse(entity);
    }

    private OrderResponse toResponse(OrderEntity orderEntity) {
        return new OrderResponse(
                orderEntity.getOrderId(),
                orderEntity.getCustomerId(),
                orderEntity.getProductId(),
                orderEntity.getQuantity(),
                orderEntity.getUnitPrice(),
                orderEntity.getTotalAmount(),
                orderEntity.getStatus(),
                orderEntity.getCreatedAt()
        );
    }

    public void rejectOrder(String orderId, Instant instant) {
        OrderEntity orderEntity = orderRepository.findById(orderId).orElseThrow();
        orderEntity.setStatus(OrderStatus.REJECTED);
        orderRepository.save(orderEntity);
    }

    public void confirmOrder(String orderId, Instant instant) {
        OrderEntity orderEntity = orderRepository.findById(orderId).orElseThrow();
        orderEntity.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(orderEntity);
    }
}

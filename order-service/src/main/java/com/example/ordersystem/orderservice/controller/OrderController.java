package com.example.ordersystem.orderservice.controller;

import com.example.ordersystem.orderservice.model.CreateOrderRequest;
import com.example.ordersystem.orderservice.model.OrderResponse;
import com.example.ordersystem.orderservice.service.OrderService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/orders")
public class OrderController {
    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public OrderResponse createOrder(@Valid @RequestBody CreateOrderRequest request) {
        log.info("Received create order request: customerId={}, productId={}, quantity={}, unitPrice={}",
                request.customerId(), request.productId(), request.quantity(), request.unitPrice());
        return orderService.createOrder(request);
    }

    @GetMapping("/{orderId}")
    public OrderResponse getOrder(@PathVariable String orderId) {
        log.info("Received get order request: orderId={}", orderId);
        return orderService.getOrder(orderId);
    }
}
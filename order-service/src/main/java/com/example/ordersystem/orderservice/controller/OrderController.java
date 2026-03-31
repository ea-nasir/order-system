package com.example.ordersystem.orderservice.controller;

import com.example.ordersystem.orderservice.model.CreateOrderRequest;
import com.example.ordersystem.orderservice.model.OrderResponse;
import com.example.ordersystem.orderservice.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public OrderResponse createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return orderService.createOrder(request);
    }

    @GetMapping("/{orderId}")
    public OrderResponse getOrder(@PathVariable String orderId) {
        return orderService.getOrder(orderId);
    }
}
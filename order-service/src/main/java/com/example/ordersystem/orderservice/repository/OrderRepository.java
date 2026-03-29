package com.example.ordersystem.orderservice.repository;

import com.example.ordersystem.orderservice.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrderEntity, String> {
}
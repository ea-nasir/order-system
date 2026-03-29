package com.example.ordersystem.orderservice.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name="Orders")
public class OrderEntity {
    @Id
    private String id;
    private String customerId;
    private BigDecimal totalAmount;
    @Enumerated
    private OrderStatus status;
    private Instant createdAt;
    public OrderEntity(){
    }
    public OrderEntity(String id, String customerId, BigDecimal totalAmount, OrderStatus status, Instant createdAt){
        this.id = id;
        this.customerId = customerId;
        this.totalAmount = totalAmount;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getId() {
        return id;
    }
}

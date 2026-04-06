package com.example.ordersystem.inventoryservice.service;

import com.example.ordersystem.sharedevents.InventoryRejectedEvent;
import com.example.ordersystem.sharedevents.PaymentFailedEvent;
import config.KafkaTopics;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class InventoryService {
    private final Map<String, Integer> stock = new HashMap<>();
    private final Map<String, Reservation> reservationMap = new HashMap<>(); //todo: impl some way to check reservations?
    private final KafkaTemplate<String, InventoryRejectedEvent> kafkaTemplate;


    public InventoryService(KafkaTemplate<String, InventoryRejectedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        initStock(stock);
    }

    public synchronized boolean reserve(String orderId, String productId, int quantity) {
        int availableStock = stock.getOrDefault(productId, 0);
        if (quantity < 1 || availableStock < quantity) {
            return false;
        }
        stock.put(productId, availableStock - quantity);
        reservationMap.put(orderId, new Reservation(productId, quantity));
        return true;
    }

    private void initStock(Map<String, Integer> stock) {
        stock.put("CHAIR", 10);
        stock.put("DESK", 5);
        stock.put("LAMP", 20); //todo: make config with properties
    }

    public synchronized void releaseReservation(String orderId) {
        Reservation reservation = reservationMap.remove(orderId);
        if (reservation == null) {
            return;
        }

        stock.merge(reservation.productId, reservation.quantity, Integer::sum);
        InventoryRejectedEvent event = new InventoryRejectedEvent(
                UUID.randomUUID(),
                Instant.now(),
                orderId,
                "Reservation released after payment failure"
        );

        kafkaTemplate.send(KafkaTopics.INVENTORY_REJECTED, orderId, event);
    }

    public Map<String, Integer> getStock() {
        return stock;
    }

    public int getItemFromStock(String productId) {
        return stock.get(productId);
    }

    private record Reservation(String productId, int quantity) {
    }
}

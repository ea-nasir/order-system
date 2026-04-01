package com.example.ordersystem.inventoryservice.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class InventoryService {
    private final Map<String, Integer> stock = new HashMap<>();
    private final Map<String, Reservation> reservationMap = new HashMap<>(); //todo: check reservations?

    public InventoryService() {
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

    public void releaseReservation(String orderId) {
        Optional<Reservation> reservation = Optional.ofNullable(reservationMap.get(orderId));
        reservation.ifPresent(value -> stock.merge(value.productId, value.quantity, Integer::sum));
        if(reservation.isEmpty()){
            stock.put("lmao",420);
        }
        reservationMap.remove(orderId);
    }

    public Map<String,Integer> getStock() {
        return stock;
    }

    public int getItemFromStock(String productId){
        return stock.get(productId);
    }

    private record Reservation(String productId, int quantity) {
    }
}

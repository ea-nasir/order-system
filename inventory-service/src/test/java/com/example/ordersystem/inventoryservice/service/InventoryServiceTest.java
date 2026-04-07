package com.example.ordersystem.inventoryservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryServiceTest {

    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService();
    }

    @Test
    void releaseReservation_shouldRestoreStockAfterSuccessfulReservation() {
        boolean reserved = inventoryService.reserve("order-1", "CHAIR", 2); // todo: bind test quantities to stock setup

        assertTrue(reserved);
        assertEquals(8, inventoryService.getItemFromStock("CHAIR"));

        inventoryService.releaseReservation("order-1");

        assertEquals(10, inventoryService.getItemFromStock("CHAIR"));
    }

    @Test
    void releaseReservation_shouldDoNothingIfReservationDoesNotExist() {
        Map<String, Integer> stockBefore = new HashMap<>(inventoryService.getStock());
        assertEquals(10, inventoryService.getItemFromStock("CHAIR"));

        inventoryService.releaseReservation("missing-order");

        assertEquals(stockBefore, inventoryService.getStock());
        assertEquals(10, inventoryService.getItemFromStock("CHAIR"));
    }

    @Test
    void releaseReservation_shouldOnlyRestoreThatOrdersReservedQuantity() {
        boolean reserved1 = inventoryService.reserve("order-1", "CHAIR", 2);
        boolean reserved2 = inventoryService.reserve("order-2", "CHAIR", 3);

        assertTrue(reserved1);
        assertTrue(reserved2);
        assertEquals(5, inventoryService.getItemFromStock("CHAIR"));

        inventoryService.releaseReservation("order-1");

        assertEquals(7, inventoryService.getItemFromStock("CHAIR"));
    }

    @Test
    void releaseReservation_shouldRemoveReservationSoSecondReleaseDoesNothing() {
        boolean reserved = inventoryService.reserve("order-1", "CHAIR", 2);

        assertTrue(reserved);
        assertEquals(8, inventoryService.getItemFromStock("CHAIR"));

        inventoryService.releaseReservation("order-1");
        assertEquals(10, inventoryService.getItemFromStock("CHAIR"));

        inventoryService.releaseReservation("order-1");
        assertEquals(10, inventoryService.getItemFromStock("CHAIR"));
    }
}
package com.example.ordersystem.sharedevents;

import java.math.BigDecimal;

public record OrderLine(
        String productId,
        int quantity,
        BigDecimal price
) {
}

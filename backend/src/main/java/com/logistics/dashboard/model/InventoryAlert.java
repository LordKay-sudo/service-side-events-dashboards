package com.logistics.dashboard.model;

public record InventoryAlert(
        String sku,
        String productName,
        String warehouseName,
        int quantityOnHand,
        int reorderLevel
) {
}

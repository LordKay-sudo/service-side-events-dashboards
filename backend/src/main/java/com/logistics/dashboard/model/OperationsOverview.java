package com.logistics.dashboard.model;

public record OperationsOverview(
        long totalOrders,
        long openOrders,
        long inTransitShipments,
        long deliveredToday,
        long delayedShipments,
        long lowStockAlerts,
        long activeVehicles
) {
}

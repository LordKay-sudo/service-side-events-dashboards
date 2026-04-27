package com.logistics.dashboard.model;

public record ShipmentStatusUpdateResponse(
        String trackingNumber,
        String status,
        boolean delayed,
        long version
) {
}

package com.logistics.dashboard.model;

import java.time.OffsetDateTime;

public record ShipmentStatus(
        String trackingNumber,
        String routeCode,
        String status,
        OffsetDateTime eta,
        boolean delayed
) {
}

package com.logistics.dashboard.model;

public record ShipmentEventIngestResponse(
        String shipmentEventId,
        boolean duplicate
) {
}

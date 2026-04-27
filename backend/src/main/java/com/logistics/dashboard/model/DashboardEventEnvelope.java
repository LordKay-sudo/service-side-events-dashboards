package com.logistics.dashboard.model;

public record DashboardEventEnvelope<T>(
        String stream,
        long sequence,
        T payload
) {
}

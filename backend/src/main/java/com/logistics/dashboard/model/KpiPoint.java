package com.logistics.dashboard.model;

import java.time.OffsetDateTime;

public record KpiPoint(
        OffsetDateTime snapshotAt,
        double onTimeDeliveryRate,
        double inventoryTurnoverRatio
) {
}

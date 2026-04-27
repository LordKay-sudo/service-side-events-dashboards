package com.logistics.dashboard.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public record ShipmentEventIngestRequest(
        @NotBlank @Size(max = 64) String trackingNumber,
        @NotBlank @Size(max = 64) String eventType,
        OffsetDateTime eventTime,
        @Size(max = 255) String eventNote
) {
}

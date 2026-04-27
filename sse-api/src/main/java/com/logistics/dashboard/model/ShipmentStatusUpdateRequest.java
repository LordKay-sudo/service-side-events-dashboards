package com.logistics.dashboard.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ShipmentStatusUpdateRequest(
        @NotBlank @Size(max = 32) String status,
        @NotNull @PositiveOrZero Long expectedVersion,
        @NotNull Boolean delayed
) {
}

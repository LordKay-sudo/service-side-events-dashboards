package com.logistics.dashboard.web;

import com.logistics.dashboard.model.ShipmentEventIngestRequest;
import com.logistics.dashboard.model.ShipmentEventIngestResponse;
import com.logistics.dashboard.service.ShipmentEventCommandService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/ingestion")
public class ShipmentIngestionController {

    private final ShipmentEventCommandService commandService;

    public ShipmentIngestionController(ShipmentEventCommandService commandService) {
        this.commandService = commandService;
    }

    @PostMapping("/shipment-events")
    public ResponseEntity<ShipmentEventIngestResponse> ingestShipmentEvent(
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @Valid @RequestBody ShipmentEventIngestRequest request) {
        ShipmentEventIngestResponse response = commandService.ingest(idempotencyKey, request);
        return ResponseEntity.ok(response);
    }
}

package com.logistics.dashboard.web;

import com.logistics.dashboard.model.ShipmentStatusUpdateRequest;
import com.logistics.dashboard.model.ShipmentStatusUpdateResponse;
import com.logistics.dashboard.service.ShipmentCommandService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping({"/api/shipments", "/api/v1/shipments"})
public class ShipmentCommandController {

    private final ShipmentCommandService commandService;

    public ShipmentCommandController(ShipmentCommandService commandService) {
        this.commandService = commandService;
    }

    @PatchMapping("/{trackingNumber}/status")
    public ShipmentStatusUpdateResponse updateStatus(
            @PathVariable @NotBlank String trackingNumber,
            @Valid @RequestBody ShipmentStatusUpdateRequest request) {
        return commandService.updateStatus(trackingNumber, request);
    }
}

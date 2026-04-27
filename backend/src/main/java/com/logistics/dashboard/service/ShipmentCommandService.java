package com.logistics.dashboard.service;

import com.logistics.dashboard.model.ShipmentStatusUpdateRequest;
import com.logistics.dashboard.model.ShipmentStatusUpdateResponse;
import com.logistics.dashboard.repository.ShipmentCommandRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShipmentCommandService {

    private final ShipmentCommandRepository repository;

    public ShipmentCommandService(ShipmentCommandRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ShipmentStatusUpdateResponse updateStatus(String trackingNumber, ShipmentStatusUpdateRequest request) {
        int updated = repository.updateStatusByTrackingNumber(
                trackingNumber,
                request.status(),
                request.delayed(),
                request.expectedVersion()
        );

        if (updated == 1) {
            return repository.fetchByTrackingNumber(trackingNumber);
        }

        if (!repository.shipmentExists(trackingNumber)) {
            throw new IllegalArgumentException("Unknown tracking number: " + trackingNumber);
        }

        throw new OptimisticLockingFailureException(
                "Shipment was modified by another request. Please refresh and retry with the latest version."
        );
    }
}

package com.logistics.dashboard.service;

import com.logistics.dashboard.model.ShipmentEventIngestRequest;
import com.logistics.dashboard.model.ShipmentEventIngestResponse;
import com.logistics.dashboard.repository.ShipmentEventCommandRepository;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShipmentEventCommandService {

    private static final String SHIPMENT_EVENT_SCOPE = "shipment-event-ingest";

    private final ShipmentEventCommandRepository repository;

    public ShipmentEventCommandService(ShipmentEventCommandRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ShipmentEventIngestResponse ingest(String idempotencyKey, ShipmentEventIngestRequest request) {
        String candidateEventId = repository.nextUuid();
        boolean reserved = repository.reserveIdempotencyKey(idempotencyKey, SHIPMENT_EVENT_SCOPE, candidateEventId);

        if (!reserved) {
            String existingEventId = repository.findResourceIdForKey(idempotencyKey);
            return new ShipmentEventIngestResponse(existingEventId, true);
        }

        String shipmentId;
        try {
            shipmentId = repository.findShipmentIdByTrackingNumber(request.trackingNumber());
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalArgumentException("Unknown tracking number: " + request.trackingNumber());
        }
        repository.insertShipmentEvent(candidateEventId, shipmentId, request);
        return new ShipmentEventIngestResponse(candidateEventId, false);
    }
}

package com.logistics.dashboard.repository;

import com.logistics.dashboard.model.ShipmentEventIngestRequest;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ShipmentEventCommandRepository {

    private final JdbcTemplate jdbcTemplate;

    public ShipmentEventCommandRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean reserveIdempotencyKey(String idempotencyKey, String scope, String resourceId) {
        String sql = """
                INSERT INTO idempotency_keys (idempotency_key, request_scope, resource_id)
                VALUES (?, ?, ?)
                """;
        try {
            return jdbcTemplate.update(sql, idempotencyKey, scope, resourceId) == 1;
        } catch (DuplicateKeyException ex) {
            return false;
        }
    }

    public String findResourceIdForKey(String idempotencyKey) {
        String sql = "SELECT resource_id FROM idempotency_keys WHERE idempotency_key = ?";
        return jdbcTemplate.queryForObject(sql, String.class, idempotencyKey);
    }

    public String findShipmentIdByTrackingNumber(String trackingNumber) {
        String sql = "SELECT id FROM shipments WHERE tracking_number = ?";
        return jdbcTemplate.queryForObject(sql, String.class, trackingNumber);
    }

    public void insertShipmentEvent(String shipmentEventId, String shipmentId, ShipmentEventIngestRequest request) {
        OffsetDateTime eventTime = request.eventTime() == null ? OffsetDateTime.now(ZoneOffset.UTC) : request.eventTime();
        String sql = """
                INSERT INTO shipment_events (id, shipment_id, event_type, event_time, event_note)
                VALUES (?, ?, ?, ?, ?)
                """;
        jdbcTemplate.update(
                sql,
                shipmentEventId,
                shipmentId,
                request.eventType(),
                Timestamp.from(eventTime.toInstant()),
                request.eventNote()
        );
    }

    public String nextUuid() {
        return UUID.randomUUID().toString();
    }
}

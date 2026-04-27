package com.logistics.dashboard.service;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "dashboards.demo-stream.enabled", havingValue = "true")
public class DashboardDataStreamingService {

    private static final List<String> TRANSIT_STATUSES = List.of("IN_TRANSIT", "PENDING_DISPATCH", "DELIVERED");

    private final JdbcTemplate jdbcTemplate;

    public DashboardDataStreamingService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(fixedDelayString = "${dashboards.demo-stream.interval-ms:4000}")
    public void streamFreshDemoData() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        updateRandomShipment(now);
        fluctuateInventory();
        appendKpiSnapshot(now);
    }

    private void updateRandomShipment(OffsetDateTime now) {
        String shipmentId = jdbcTemplate.query(
                "SELECT id FROM shipments ORDER BY RAND() LIMIT 1",
                rs -> rs.next() ? rs.getString("id") : null
        );

        if (shipmentId == null) {
            return;
        }

        String status = TRANSIT_STATUSES.get(ThreadLocalRandom.current().nextInt(TRANSIT_STATUSES.size()));
        boolean delayed = ThreadLocalRandom.current().nextBoolean();
        OffsetDateTime eta = now.plusHours(ThreadLocalRandom.current().nextLong(1, 18));
        Timestamp deliveredAt = "DELIVERED".equals(status) ? Timestamp.from(now.toInstant()) : null;

        jdbcTemplate.update(
                """
                UPDATE shipments
                SET shipment_status = ?, is_delayed = ?, eta = ?, delivered_at = ?, lock_version = lock_version + 1
                WHERE id = ?
                """,
                status,
                delayed,
                Timestamp.from(eta.toInstant()),
                deliveredAt,
                shipmentId
        );
    }

    private void fluctuateInventory() {
        String inventoryId = jdbcTemplate.query(
                "SELECT id FROM inventory_levels ORDER BY RAND() LIMIT 1",
                rs -> rs.next() ? rs.getString("id") : null
        );

        if (inventoryId == null) {
            return;
        }

        int delta = ThreadLocalRandom.current().nextInt(-8, 9);
        jdbcTemplate.update(
                """
                UPDATE inventory_levels
                SET quantity_on_hand = GREATEST(0, quantity_on_hand + ?),
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """,
                delta,
                inventoryId
        );
    }

    private void appendKpiSnapshot(OffsetDateTime now) {
        double onTime = clamp(ThreadLocalRandom.current().nextDouble(88.0, 98.0), 85.0, 99.5);
        double turnover = clamp(ThreadLocalRandom.current().nextDouble(2.8, 4.3), 2.0, 5.5);

        jdbcTemplate.update(
                """
                INSERT INTO kpi_snapshots (id, snapshot_at, on_time_delivery_rate, inventory_turnover_ratio)
                VALUES (?, ?, ?, ?)
                """,
                java.util.UUID.randomUUID().toString(),
                Timestamp.from(now.toInstant()),
                onTime,
                turnover
        );
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}

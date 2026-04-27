package com.logistics.dashboard.repository;

import com.logistics.dashboard.model.ShipmentStatusUpdateResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ShipmentCommandRepository {

    private final JdbcTemplate jdbcTemplate;

    public ShipmentCommandRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int updateStatusByTrackingNumber(String trackingNumber, String status, boolean delayed, long expectedVersion) {
        String sql = """
                UPDATE shipments
                SET status = ?, delayed = ?, version = version + 1
                WHERE tracking_number = ? AND version = ?
                """;
        return jdbcTemplate.update(sql, status, delayed, trackingNumber, expectedVersion);
    }

    public boolean shipmentExists(String trackingNumber) {
        String sql = "SELECT COUNT(*) FROM shipments WHERE tracking_number = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, trackingNumber);
        return count != null && count > 0;
    }

    public ShipmentStatusUpdateResponse fetchByTrackingNumber(String trackingNumber) {
        String sql = """
                SELECT tracking_number, status, delayed, version
                FROM shipments
                WHERE tracking_number = ?
                """;
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new ShipmentStatusUpdateResponse(
                rs.getString("tracking_number"),
                rs.getString("status"),
                rs.getBoolean("delayed"),
                rs.getLong("version")
        ), trackingNumber);
    }
}

package com.logistics.dashboard.repository;

import com.logistics.dashboard.model.InventoryAlert;
import com.logistics.dashboard.model.KpiPoint;
import com.logistics.dashboard.model.OperationsOverview;
import com.logistics.dashboard.model.ShipmentStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DashboardQueryRepository {

    private final JdbcTemplate jdbcTemplate;

    public DashboardQueryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public OperationsOverview fetchOverview() {
        String sql = """
                SELECT
                  (SELECT COUNT(*) FROM customer_orders) AS total_orders,
                  (SELECT COUNT(*) FROM customer_orders WHERE order_status IN ('CREATED', 'PICKING', 'PACKING')) AS open_orders,
                  (SELECT COUNT(*) FROM shipments WHERE shipment_status = 'IN_TRANSIT') AS in_transit_shipments,
                  (SELECT COUNT(*) FROM shipments WHERE shipment_status = 'DELIVERED' AND delivered_at IS NOT NULL) AS delivered_today,
                  (SELECT COUNT(*) FROM shipments WHERE is_delayed = TRUE) AS delayed_shipments,
                  (SELECT COUNT(*) FROM inventory_levels WHERE quantity_on_hand <= reorder_level) AS low_stock_alerts,
                  (SELECT COUNT(*) FROM fleet_vehicles WHERE operational_status = 'ACTIVE') AS active_vehicles
                """;

        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new OperationsOverview(
                rs.getLong("total_orders"),
                rs.getLong("open_orders"),
                rs.getLong("in_transit_shipments"),
                rs.getLong("delivered_today"),
                rs.getLong("delayed_shipments"),
                rs.getLong("low_stock_alerts"),
                rs.getLong("active_vehicles")
        ));
    }

    public List<InventoryAlert> fetchInventoryAlerts(int limit) {
        String sql = """
                SELECT p.sku,
                       p.product_name,
                       w.name AS warehouse_name,
                       il.quantity_on_hand,
                       il.reorder_level
                FROM inventory_levels il
                JOIN products p ON p.id = il.product_id
                JOIN warehouses w ON w.id = il.warehouse_id
                WHERE il.quantity_on_hand <= il.reorder_level
                ORDER BY (il.reorder_level - il.quantity_on_hand) DESC, p.product_name ASC
                LIMIT ?
                """;
        return jdbcTemplate.query(sql, this::mapInventoryAlert, limit);
    }

    public List<ShipmentStatus> fetchShipmentStatuses(int limit) {
        String sql = """
                SELECT tracking_number,
                       route_code,
                       shipment_status,
                       eta,
                       is_delayed AS delayed
                FROM shipments
                ORDER BY is_delayed DESC, eta ASC
                LIMIT ?
                """;
        return jdbcTemplate.query(sql, this::mapShipmentStatus, limit);
    }

    public List<KpiPoint> fetchRecentKpiTrend(int limit) {
        String sql = """
                SELECT snapshot_at,
                       on_time_delivery_rate,
                       inventory_turnover_ratio
                FROM kpi_snapshots
                ORDER BY snapshot_at DESC
                LIMIT ?
                """;
        return jdbcTemplate.query(sql, this::mapKpiPoint, limit);
    }

    private InventoryAlert mapInventoryAlert(ResultSet rs, int rowNum) throws SQLException {
        return new InventoryAlert(
                rs.getString("sku"),
                rs.getString("product_name"),
                rs.getString("warehouse_name"),
                rs.getInt("quantity_on_hand"),
                rs.getInt("reorder_level")
        );
    }

    private ShipmentStatus mapShipmentStatus(ResultSet rs, int rowNum) throws SQLException {
        return new ShipmentStatus(
                rs.getString("tracking_number"),
                rs.getString("route_code"),
                rs.getString("shipment_status"),
                rs.getObject("eta", OffsetDateTime.class),
                rs.getBoolean("delayed")
        );
    }

    private KpiPoint mapKpiPoint(ResultSet rs, int rowNum) throws SQLException {
        return new KpiPoint(
                rs.getObject("snapshot_at", OffsetDateTime.class),
                rs.getDouble("on_time_delivery_rate"),
                rs.getDouble("inventory_turnover_ratio")
        );
    }
}

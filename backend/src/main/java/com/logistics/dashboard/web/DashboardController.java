package com.logistics.dashboard.web;

import com.logistics.dashboard.model.InventoryAlert;
import com.logistics.dashboard.model.KpiPoint;
import com.logistics.dashboard.model.OperationsOverview;
import com.logistics.dashboard.model.ShipmentStatus;
import com.logistics.dashboard.service.DashboardSnapshotService;
import com.logistics.dashboard.service.DashboardStreamService;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping({"/api/dashboards", "/api/v1/dashboards"})
public class DashboardController {

    private final DashboardSnapshotService snapshots;
    private final DashboardStreamService streamService;

    public DashboardController(DashboardSnapshotService snapshots, DashboardStreamService streamService) {
        this.snapshots = snapshots;
        this.streamService = streamService;
    }

    @GetMapping("/overview")
    public OperationsOverview getOverview() {
        return snapshots.getOverview();
    }

    @GetMapping("/inventory-alerts")
    public List<InventoryAlert> getInventoryAlerts() {
        return snapshots.getInventoryAlerts(12);
    }

    @GetMapping("/shipment-status")
    public List<ShipmentStatus> getShipmentStatuses() {
        return snapshots.getShipmentStatuses(15);
    }

    @GetMapping("/kpi-trend")
    public List<KpiPoint> getKpiTrend() {
        return snapshots.getKpiTrend(20);
    }

    @GetMapping(path = "/stream/overview", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> streamOverview(
            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId) {
        return streamResponse(streamService.subscribe("overview", lastEventId));
    }

    @GetMapping(path = "/stream/inventory-alerts", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> streamInventoryAlerts(
            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId) {
        return streamResponse(streamService.subscribe("inventory-alerts", lastEventId));
    }

    @GetMapping(path = "/stream/shipment-status", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> streamShipmentStatus(
            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId) {
        return streamResponse(streamService.subscribe("shipment-status", lastEventId));
    }

    @GetMapping(path = "/stream/kpi-trend", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> streamKpiTrend(
            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId) {
        return streamResponse(streamService.subscribe("kpi-trend", lastEventId));
    }

    private ResponseEntity<SseEmitter> streamResponse(SseEmitter emitter) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .header(HttpHeaders.CONNECTION, "keep-alive")
                .header("X-Accel-Buffering", "no")
                .body(emitter);
    }
}

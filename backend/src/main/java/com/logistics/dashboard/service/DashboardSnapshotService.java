package com.logistics.dashboard.service;

import com.logistics.dashboard.model.InventoryAlert;
import com.logistics.dashboard.model.KpiPoint;
import com.logistics.dashboard.model.OperationsOverview;
import com.logistics.dashboard.model.ShipmentStatus;
import com.logistics.dashboard.repository.DashboardQueryRepository;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class DashboardSnapshotService {

    private final DashboardQueryRepository repository;

    public DashboardSnapshotService(DashboardQueryRepository repository) {
        this.repository = repository;
    }

    @Cacheable(cacheNames = "overview")
    public OperationsOverview getOverview() {
        return repository.fetchOverview();
    }

    @Cacheable(cacheNames = "inventory-alerts")
    public List<InventoryAlert> getInventoryAlerts(int limit) {
        return repository.fetchInventoryAlerts(limit);
    }

    @Cacheable(cacheNames = "shipment-status")
    public List<ShipmentStatus> getShipmentStatuses(int limit) {
        return repository.fetchShipmentStatuses(limit);
    }

    @Cacheable(cacheNames = "kpi-trend")
    public List<KpiPoint> getKpiTrend(int limit) {
        return repository.fetchRecentKpiTrend(limit);
    }
}

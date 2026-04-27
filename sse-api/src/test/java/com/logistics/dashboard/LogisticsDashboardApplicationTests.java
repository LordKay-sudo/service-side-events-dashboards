package com.logistics.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.dashboard.model.OperationsOverview;
import com.logistics.dashboard.service.DashboardSnapshotService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class LogisticsDashboardApplicationTests {

    @Autowired
    private DashboardSnapshotService snapshotService;

    @Test
    void contextLoadsAndCanReadOverview() {
        OperationsOverview overview = snapshotService.getOverview();
        assertThat(overview.totalOrders()).isGreaterThan(0);
        assertThat(overview.activeVehicles()).isGreaterThan(0);
    }
}

package com.logistics.dashboard.service;

import com.logistics.dashboard.model.DashboardEventEnvelope;
import com.logistics.dashboard.model.InventoryAlert;
import com.logistics.dashboard.model.KpiPoint;
import com.logistics.dashboard.model.OperationsOverview;
import com.logistics.dashboard.model.ShipmentStatus;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class DashboardStreamService {

    private final DashboardSnapshotService snapshots;
    private final AtomicLong sequence = new AtomicLong();
    private final Map<String, CopyOnWriteArrayList<SseEmitter>> emittersByStream = new ConcurrentHashMap<>();

    @Value("${dashboards.stream.timeout-ms:0}")
    private long timeoutMs;

    public DashboardStreamService(DashboardSnapshotService snapshots) {
        this.snapshots = snapshots;
    }

    public SseEmitter subscribe(String streamName, String lastEventId) {
        SseEmitter emitter = new SseEmitter(timeoutMs);
        emittersByStream.computeIfAbsent(streamName, ignored -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(streamName, emitter));
        emitter.onTimeout(() -> removeEmitter(streamName, emitter));
        emitter.onError(ex -> removeEmitter(streamName, emitter));

        sendReconnectHint(streamName, emitter, lastEventId);
        sendInitialSnapshot(streamName, emitter);
        return emitter;
    }

    @Scheduled(fixedDelayString = "${dashboards.stream.interval-ms:5000}")
    @CacheEvict(cacheNames = {"overview", "inventory-alerts", "shipment-status", "kpi-trend"}, allEntries = true)
    public void refreshAndBroadcast() {
        broadcast("overview", snapshots.getOverview());
        broadcast("inventory-alerts", snapshots.getInventoryAlerts(12));
        broadcast("shipment-status", snapshots.getShipmentStatuses(15));
        broadcast("kpi-trend", snapshots.getKpiTrend(20));
    }

    @Scheduled(fixedDelayString = "${dashboards.stream.heartbeat-ms:15000}")
    public void heartbeat() {
        for (Map.Entry<String, CopyOnWriteArrayList<SseEmitter>> entry : emittersByStream.entrySet()) {
            String streamName = entry.getKey();
            List<SseEmitter> snapshot = new ArrayList<>(entry.getValue());
            for (SseEmitter emitter : snapshot) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("heartbeat")
                            .comment("keep-alive:" + streamName));
                } catch (IOException ex) {
                    removeEmitter(streamName, emitter);
                    emitter.completeWithError(ex);
                }
            }
        }
    }

    private void sendReconnectHint(String streamName, SseEmitter emitter, String lastEventId) {
        if (lastEventId == null || lastEventId.isBlank()) {
            return;
        }
        try {
            emitter.send(SseEmitter.event()
                    .name("resume")
                    .data("reconnected from event id " + lastEventId + " for stream " + streamName));
        } catch (IOException ex) {
            removeEmitter(streamName, emitter);
            emitter.completeWithError(ex);
        }
    }

    private void sendInitialSnapshot(String streamName, SseEmitter emitter) {
        try {
            switch (streamName) {
                case "overview" -> send(emitter, "overview", snapshots.getOverview());
                case "inventory-alerts" -> send(emitter, "inventory-alerts", snapshots.getInventoryAlerts(12));
                case "shipment-status" -> send(emitter, "shipment-status", snapshots.getShipmentStatuses(15));
                case "kpi-trend" -> send(emitter, "kpi-trend", snapshots.getKpiTrend(20));
                default -> {
                    emitter.completeWithError(new IllegalArgumentException("Unsupported stream: " + streamName));
                    removeEmitter(streamName, emitter);
                }
            }
        } catch (IOException ex) {
            removeEmitter(streamName, emitter);
            emitter.completeWithError(ex);
        }
    }

    private <T> void broadcast(String streamName, T payload) {
        List<SseEmitter> emitters = emittersByStream.getOrDefault(streamName, new CopyOnWriteArrayList<>());
        Iterator<SseEmitter> iterator = emitters.iterator();
        while (iterator.hasNext()) {
            SseEmitter emitter = iterator.next();
            try {
                send(emitter, streamName, payload);
            } catch (IOException ex) {
                removeEmitter(streamName, emitter);
                emitter.completeWithError(ex);
            }
        }
    }

    private <T> void send(SseEmitter emitter, String streamName, T payload) throws IOException {
        long nextSequence = sequence.incrementAndGet();
        DashboardEventEnvelope<T> envelope = new DashboardEventEnvelope<>(streamName, nextSequence, payload);
        emitter.send(SseEmitter.event()
                .name(streamName)
                .id(Long.toString(nextSequence))
                .reconnectTime(Duration.ofSeconds(2).toMillis())
                .data(envelope));
    }

    private void removeEmitter(String streamName, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersByStream.get(streamName);
        if (emitters != null) {
            emitters.remove(emitter);
        }
    }
}

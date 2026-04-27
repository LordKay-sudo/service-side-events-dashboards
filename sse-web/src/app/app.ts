import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, computed, effect, signal } from '@angular/core';

interface OperationsOverview {
  totalOrders: number;
  openOrders: number;
  inTransitShipments: number;
  deliveredToday: number;
  delayedShipments: number;
  lowStockAlerts: number;
  activeVehicles: number;
}

interface InventoryAlert {
  sku: string;
  productName: string;
  warehouseName: string;
  quantityOnHand: number;
  reorderLevel: number;
}

interface ShipmentStatus {
  trackingNumber: string;
  routeCode: string;
  status: string;
  eta: string;
  delayed: boolean;
}

interface KpiPoint {
  snapshotAt: string;
  onTimeDeliveryRate: number;
  inventoryTurnoverRatio: number;
}

interface DashboardEventEnvelope<T> {
  stream: string;
  sequence: number;
  payload: T;
}

@Component({
  selector: 'app-root',
  imports: [CommonModule],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App implements OnInit, OnDestroy {
  protected readonly title = 'Logistics Live Dashboards';
  protected readonly apiBase = 'http://localhost:8080/api/dashboards';
  protected readonly connected = signal(false);
  protected readonly overview = signal<OperationsOverview | null>(null);
  protected readonly inventoryAlerts = signal<InventoryAlert[]>([]);
  protected readonly shipmentStatuses = signal<ShipmentStatus[]>([]);
  protected readonly kpiTrend = signal<KpiPoint[]>([]);
  protected readonly delayedShipments = computed(
    () => this.shipmentStatuses().filter((shipment) => shipment.delayed).length
  );
  protected readonly streamSummary = computed(
    () =>
      `${this.inventoryAlerts().length} inventory alerts · ` +
      `${this.shipmentStatuses().length} shipment rows · ` +
      `${this.kpiTrend().length} KPI points`
  );

  private readonly streams: EventSource[] = [];

  constructor() {
    effect(() => {
      if (this.connected()) {
        // A tiny effect for visibility during live updates.
        // eslint-disable-next-line no-console
        console.debug('SSE connected:', this.streamSummary());
      }
    });
  }

  ngOnInit(): void {
    void this.loadInitialSnapshots();

    this.openStream<OperationsOverview>('overview', (payload) => {
      this.overview.set(payload);
    });
    this.openStream<InventoryAlert[]>('inventory-alerts', (payload) => {
      this.inventoryAlerts.set(payload);
    });
    this.openStream<ShipmentStatus[]>('shipment-status', (payload) => {
      this.shipmentStatuses.set(payload);
    });
    this.openStream<KpiPoint[]>('kpi-trend', (payload) => {
      this.kpiTrend.set(payload);
    });
  }

  ngOnDestroy(): void {
    for (const stream of this.streams) {
      stream.close();
    }
  }

  private openStream<T>(streamName: string, onPayload: (payload: T) => void): void {
    const source = new EventSource(`${this.apiBase}/stream/${streamName}`);
    source.onopen = () => {
      this.connected.set(true);
    };
    source.onerror = () => {
      this.connected.set(false);
    };
    const consumePayload = (event: MessageEvent<string>) => {
      try {
        const envelope = JSON.parse(event.data) as DashboardEventEnvelope<T>;
        onPayload(envelope.payload);
      } catch (_error) {
        this.connected.set(false);
      }
    };

    // Backend emits named SSE events (overview, inventory-alerts, etc.),
    // so subscribe explicitly to each stream name.
    source.addEventListener(streamName, (event) => {
      consumePayload(event as MessageEvent<string>);
    });

    // Keep default handler for compatibility if unnamed "message" events are used.
    source.onmessage = (event) => consumePayload(event);

    this.streams.push(source);
  }

  private async loadInitialSnapshots(): Promise<void> {
    const [overview, inventoryAlerts, shipmentStatuses, kpiTrend] = await Promise.all([
      this.loadJson<OperationsOverview>('/overview'),
      this.loadJson<InventoryAlert[]>('/inventory-alerts'),
      this.loadJson<ShipmentStatus[]>('/shipment-status'),
      this.loadJson<KpiPoint[]>('/kpi-trend')
    ]);

    if (overview) {
      this.overview.set(overview);
    }
    this.inventoryAlerts.set(inventoryAlerts ?? []);
    this.shipmentStatuses.set(shipmentStatuses ?? []);
    this.kpiTrend.set(kpiTrend ?? []);
  }

  private async loadJson<T>(path: string): Promise<T | null> {
    try {
      const response = await fetch(`${this.apiBase}${path}`);
      if (!response.ok) {
        return null;
      }
      return (await response.json()) as T;
    } catch (_error) {
      return null;
    }
  }
}

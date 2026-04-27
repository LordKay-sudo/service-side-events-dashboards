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
    source.onmessage = (event) => {
      try {
        const envelope = JSON.parse(event.data) as DashboardEventEnvelope<T>;
        onPayload(envelope.payload);
      } catch (_error) {
        this.connected.set(false);
      }
    };
    this.streams.push(source);
  }
}

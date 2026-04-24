import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { BaseChartDirective } from 'ng2-charts';
import { ChartData, ChartOptions, ChartEvent, ActiveElement, Plugin } from 'chart.js';
import {
  Chart,
  LineController,
  LineElement,
  PointElement,
  LinearScale,
  TimeScale,
  Tooltip,
  Legend,
  Filler,
} from 'chart.js';
import 'chartjs-adapter-date-fns';
import { enUS, ptBR } from 'date-fns/locale';
import { forkJoin } from 'rxjs';
import { IopsService } from '../../services/iops.service';
import { IopsQueryResponse, Granularity } from '../../models/iops.model';

Chart.register(
  LineController,
  LineElement,
  PointElement,
  LinearScale,
  TimeScale,
  Tooltip,
  Legend,
  Filler
);

interface NodeCard {
  response: IopsQueryResponse;
  chartData: ChartData<'line'>;
  chartOptions: ChartOptions<'line'>;
  plugins: Plugin[];
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [FormsModule, BaseChartDirective],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardComponent implements OnInit {
  private readonly iopsService = inject(IopsService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly router = inject(Router);

  granularities: Granularity[] = ['PT1M', 'PT5M', 'PT1H', 'P1D'];
  selectedGranularity: Granularity = 'PT1H';
  startDate = '';
  endDate = '';
  locale: 'en-US' | 'pt-BR' = 'pt-BR';
  showRead = true;
  showWrite = true;
  showTotal = false;
  private response: IopsQueryResponse | null = null;
  private replicaResponses: IopsQueryResponse[] = [];
  card: NodeCard | null = null;
  replicaCards: NodeCard[] = [];
  loading = false;
  error = '';
  readonly chartType = 'line' as const;

  ngOnInit(): void {
    const today = new Date();
    const start = new Date(today.getFullYear(), today.getMonth(), today.getDate(), 0, 0);
    const end = new Date(today.getFullYear(), today.getMonth(), today.getDate(), 23, 59);
    this.startDate = this.toDatetimeLocal(start);
    this.endDate = this.toDatetimeLocal(end);
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.card = null;
    this.replicaCards = [];
    const start = new Date(this.startDate).toISOString();
    const end = new Date(this.endDate).toISOString();
    forkJoin({
      primary: this.iopsService.queryPrimaryIops(this.selectedGranularity, start, end),
      replicas: this.iopsService.queryIops(this.selectedGranularity, start, end),
    }).subscribe({
      next: ({ primary, replicas }) => {
        this.response = primary;
        this.card = this.buildCard(primary);
        this.replicaResponses = replicas;
        this.replicaCards = replicas.map((r) => this.buildCard(r));
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err.message ?? 'Failed to load data';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  onVisibilityChange(): void {
    if (this.response) {
      this.card = this.buildCard(this.response);
    }
    if (this.replicaResponses.length) {
      this.replicaCards = this.replicaResponses.map((r) => this.buildCard(r));
    }
    if (this.response || this.replicaResponses.length) {
      this.cdr.markForCheck();
    }
  }

  onLocaleChange(): void {
    if (this.response) {
      this.card = this.buildCard(this.response);
    }
    if (this.replicaResponses.length) {
      this.replicaCards = this.replicaResponses.map((r) => this.buildCard(r));
    }
    if (this.response || this.replicaResponses.length) {
      this.cdr.markForCheck();
    }
  }

  formatDate(dateStr: string): string {
    return new Intl.DateTimeFormat(this.locale, { dateStyle: 'short', timeStyle: 'short' }).format(
      new Date(dateStr)
    );
  }

  private buildCard(r: IopsQueryResponse): NodeCard {
    const labels = r.read.dataPoints.map((dp) => new Date(dp.timestamp));
    const chartData: ChartData<'line'> = {
      labels,
      datasets: [
        {
          label: 'Read',
          data: r.read.dataPoints.map((dp) => dp.value),
          borderColor: '#60a5fa',
          backgroundColor: 'rgba(96,165,250,0.12)',
          borderWidth: 2,
          pointRadius: 0,
          pointHoverRadius: 4,
          tension: 0,
          fill: false,
          hidden: !this.showRead,
        },
        {
          label: 'Write',
          data: r.write.dataPoints.map((dp) => dp.value),
          borderColor: '#fb923c',
          backgroundColor: 'rgba(251,146,60,0.12)',
          borderWidth: 2,
          pointRadius: 0,
          pointHoverRadius: 4,
          tension: 0,
          fill: false,
          hidden: !this.showWrite,
        },
        {
          label: 'Total',
          data: r.total.dataPoints.map((dp) => dp.value),
          borderColor: '#4ade80',
          backgroundColor: 'rgba(74,222,128,0.12)',
          borderWidth: 2,
          pointRadius: 0,
          pointHoverRadius: 4,
          tension: 0,
          fill: false,
          hidden: !this.showTotal,
        },
      ],
    };

    const chartOptions: ChartOptions<'line'> = {
      animation: false,
      responsive: true,
      maintainAspectRatio: false,
      onClick: (_event: ChartEvent, _elements: ActiveElement[], chart: Chart) => {
        const xScale = chart.scales['x'];
        if (!xScale || !(_event.native instanceof MouseEvent)) return;
        const rect = chart.canvas.getBoundingClientRect();
        const xPixel = (_event.native as MouseEvent).clientX - rect.left;
        const timestamp = xScale.getValueForPixel(xPixel);
        if (timestamp == null) return;
        const clicked = new Date(timestamp);
        const start = new Date(clicked.getTime() - 3600_000);
        const end = new Date(clicked.getTime() + 3600_000);
        this.router.navigate(['/slow-queries'], {
          queryParams: { start: start.toISOString(), end: end.toISOString() },
        });
      },
      plugins: {
        legend: {
          position: 'top',
          labels: { color: '#94a3b8', boxWidth: 12, padding: 16 },
        },
        tooltip: {
          mode: 'index',
          intersect: false,
          backgroundColor: '#1a2540',
          borderColor: '#263452',
          borderWidth: 1,
          titleColor: '#e2e8f0',
          bodyColor: '#94a3b8',
        },
      },
      scales: {
        x: {
          type: 'time',
          adapters: { date: { locale: this.locale === 'pt-BR' ? ptBR : enUS } },
          time: { tooltipFormat: 'PPpp' },
          ticks: { maxTicksLimit: 8, color: '#4e6080' },
          grid: { color: '#1e2d47' },
          border: { color: '#263452' },
        },
        y: {
          beginAtZero: true,
          title: { display: true, text: 'IOPS', color: '#4e6080' },
          ticks: { color: '#4e6080' },
          grid: { color: '#1e2d47' },
          border: { color: '#263452' },
        },
      },
    };

    return { response: r, chartData, chartOptions, plugins: this.makeRoleChangePlugin(r.roleChanges) };
  }

  private makeRoleChangePlugin(roleChanges: string[]): Plugin[] {
    if (!roleChanges?.length) return [];
    return [{
      id: 'roleChangeLines',
      afterDraw(chart) {
        const { ctx, scales } = chart;
        const xScale = scales['x'];
        const yScale = scales['y'];
        if (!xScale || !yScale) return;

        ctx.save();
        ctx.strokeStyle = '#f59e0b';
        ctx.lineWidth = 1.5;
        ctx.setLineDash([5, 4]);

        for (const ts of roleChanges) {
          const x = xScale.getPixelForValue(new Date(ts).getTime());
          ctx.beginPath();
          ctx.moveTo(x, yScale.top);
          ctx.lineTo(x, yScale.bottom);
          ctx.stroke();
        }

        ctx.setLineDash([]);
        ctx.font = 'bold 10px Inter, system-ui, sans-serif';
        ctx.fillStyle = '#f59e0b';
        ctx.textAlign = 'left';

        for (const ts of roleChanges) {
          const x = xScale.getPixelForValue(new Date(ts).getTime());
          ctx.fillText('Failover', x + 4, yScale.top + 12);
        }

        ctx.restore();
      },
    }];
  }

  private toDatetimeLocal(date: Date): string {
    const pad = (n: number) => String(n).padStart(2, '0');
    return (
      `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}` +
      `T${pad(date.getHours())}:${pad(date.getMinutes())}`
    );
  }

  formatPeak(value: number | undefined, timestamp: string | undefined): string {
    if (value == null || timestamp == null) return '—';
    return `${value.toLocaleString(this.locale)} @ ${new Date(timestamp).toLocaleString(this.locale)}`;
  }
}

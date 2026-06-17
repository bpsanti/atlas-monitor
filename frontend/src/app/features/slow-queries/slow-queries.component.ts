import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { BaseChartDirective } from 'ng2-charts';
import { ChartData, ChartOptions, ActiveElement, ChartEvent } from 'chart.js';
import {
  Chart,
  BubbleController,
  PointElement,
  LinearScale,
  TimeScale,
  Tooltip,
  Legend,
} from 'chart.js';
import 'chartjs-adapter-date-fns';
import { enUS, ptBR } from 'date-fns/locale';
import { marked } from 'marked';
import { SlowQueryService } from '../../services/slow-query.service';
import { SlowQueryResponse, SlowQueryAnalysisResponse } from '../../models/slow-query.model';

Chart.register(BubbleController, PointElement, LinearScale, TimeScale, Tooltip, Legend);

@Component({
  selector: 'app-slow-queries',
  standalone: true,
  imports: [FormsModule, BaseChartDirective],
  templateUrl: './slow-queries.component.html',
  styleUrl: './slow-queries.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SlowQueriesComponent implements OnInit {
  private readonly slowQueryService = inject(SlowQueryService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly route = inject(ActivatedRoute);

  startDate = '';
  endDate = '';
  minDurationMillis: number | null = 2000;
  locale: 'en-US' | 'pt-BR' = 'pt-BR';
  loading = false;
  error = '';
  queries: SlowQueryResponse[] = [];
  sortedQueries: SlowQueryResponse[] = [];
  sortColumn: 'occurredAt' | 'durationMillis' = 'occurredAt';
  sortDirection: 'asc' | 'desc' = 'desc';
  selectedQuery: SlowQueryResponse | null = null;
  activeTab: 'details' | 'analysis' = 'details';
  databaseAnalysisHtml: string | null = null;
  analysisMetadata: SlowQueryAnalysisResponse | null = null;
  analyzing = false;
  loadingAnalysis = false;
  chartData: ChartData<'bubble'> | null = null;
  chartOptions: ChartOptions<'bubble'> | null = null;
  readonly chartType = 'bubble' as const;
  private flatQueryIndex: SlowQueryResponse[] = [];

  ngOnInit(): void {
    const params = this.route.snapshot.queryParams;
    if (params['start'] && params['end']) {
      this.startDate = this.toDatetimeLocal(new Date(params['start']));
      this.endDate = this.toDatetimeLocal(new Date(params['end']));
    } else {
      const today = new Date();
      const start = new Date(today.getFullYear(), today.getMonth(), today.getDate(), 0, 0);
      const end = new Date(today.getFullYear(), today.getMonth(), today.getDate(), 23, 59);
      this.startDate = this.toDatetimeLocal(start);
      this.endDate = this.toDatetimeLocal(end);
    }
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.chartData = null;

    const startDate = new Date(this.startDate).toISOString();
    const endDate = new Date(this.endDate).toISOString();

    this.slowQueryService
      .getSlowQueries(startDate, endDate, this.minDurationMillis ?? undefined)
      .subscribe({
        next: (data) => {
          this.queries = data;
          this.sortQueries();
          this.buildChart(data);
          this.loading = false;
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.error = err.message ?? 'Failed to load slow queries';
          this.loading = false;
          this.cdr.markForCheck();
        },
      });
  }

  private buildChart(data: SlowQueryResponse[]): void {
    const namespaceMap = new Map<string, SlowQueryResponse[]>();
    for (const q of data) {
      const ns = q.namespace ?? 'unknown';
      if (!namespaceMap.has(ns)) namespaceMap.set(ns, []);
      namespaceMap.get(ns)!.push(q);
    }

    const colors = ['#60a5fa', '#fb923c', '#4ade80', '#f472b6', '#a78bfa', '#facc15', '#34d399', '#f87171'];
    let colorIdx = 0;

    this.flatQueryIndex = [];

    const datasets = Array.from(namespaceMap.entries()).map(([ns, queries]) => {
      const color = colors[colorIdx++ % colors.length];
      for (const q of queries) {
        this.flatQueryIndex.push(q);
      }
      return {
        label: ns,
        data: queries.map((q) => ({
          x: new Date(q.occurredAt).getTime(),
          y: q.durationMillis / 1000,
          r: 6,
        })),
        backgroundColor: color + '99',
        borderColor: color,
        borderWidth: 1,
      };
    });

    this.chartData = { datasets };

    this.chartOptions = {
      animation: false,
      responsive: true,
      maintainAspectRatio: false,
      onHover: (_event: ChartEvent, elements: ActiveElement[], chart: Chart) => {
        chart.canvas.style.cursor = elements.length ? 'pointer' : 'default';
      },
      onClick: (_event: ChartEvent, elements: ActiveElement[], chart: Chart) => {
        if (!elements.length) return;
        const el = elements[0];
        let globalIdx = 0;
        for (let d = 0; d < el.datasetIndex; d++) {
          globalIdx += chart.data.datasets[d].data.length;
        }
        globalIdx += el.index;
        const query = this.flatQueryIndex[globalIdx];
        if (query) {
          this.selectQuery(query);
        }
      },
      plugins: {
        legend: {
          position: 'top',
          labels: { color: '#94a3b8', boxWidth: 12, padding: 16 },
        },
        tooltip: {
          backgroundColor: '#1a2540',
          borderColor: '#263452',
          borderWidth: 1,
          titleColor: '#e2e8f0',
          bodyColor: '#94a3b8',
          callbacks: {
            label: (ctx) => {
              const raw = ctx.raw as { x: number; y: number };
              const date = new Intl.DateTimeFormat(this.locale, {
                dateStyle: 'short',
                timeStyle: 'medium',
              }).format(new Date(raw.x));
              return `${ctx.dataset.label} — ${raw.y.toFixed(2)}s @ ${date}`;
            },
          },
        },
      },
      scales: {
        x: {
          type: 'time',
          adapters: { date: { locale: this.locale === 'pt-BR' ? ptBR : enUS } },
          time: { tooltipFormat: 'PPpp' },
          ticks: { maxTicksLimit: 10, color: '#4e6080' },
          grid: { color: '#1e2d47' },
          border: { color: '#263452' },
          title: { display: true, text: 'Time', color: '#4e6080' },
        },
        y: {
          beginAtZero: true,
          ticks: { color: '#4e6080' },
          grid: { color: '#1e2d47' },
          border: { color: '#263452' },
          title: { display: true, text: 'Duration (s)', color: '#4e6080' },
        },
      },
    };
  }

  selectQuery(query: SlowQueryResponse): void {
    this.selectedQuery = query;
    this.activeTab = 'details';
    this.databaseAnalysisHtml = null;
    this.analysisMetadata = null;
    this.analyzing = false;
    this.loadingAnalysis = true;
    this.cdr.markForCheck();

    this.slowQueryService.findAnalysis(query).subscribe({
      next: (res) => {
        if (res) {
          this.analysisMetadata = res;
          this.databaseAnalysisHtml = res.databaseAnalysis
            ? marked.parse(res.databaseAnalysis) as string
            : null;
        }
        this.loadingAnalysis = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.loadingAnalysis = false;
        this.cdr.markForCheck();
      },
    });
  }

  closePanel(): void {
    this.selectedQuery = null;
    this.activeTab = 'details';
    this.databaseAnalysisHtml = null;
    this.analysisMetadata = null;
    this.analyzing = false;
    this.loadingAnalysis = false;
    this.cdr.markForCheck();
  }

  analyzeQuery(): void {
    if (!this.selectedQuery || this.analyzing) return;
    this.analyzing = true;
    this.databaseAnalysisHtml = null;
    this.analysisMetadata = null;
    this.slowQueryService.analyzeQuery(this.selectedQuery, true).subscribe({
      next: (res) => {
        this.analysisMetadata = res;
        this.databaseAnalysisHtml = res.databaseAnalysis
          ? marked.parse(res.databaseAnalysis) as string
          : null;
        this.analyzing = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.databaseAnalysisHtml = '<p>Failed to analyze query.</p>';
        this.analyzing = false;
        this.cdr.markForCheck();
      },
    });
  }

  renderMarkdown(text: string): string {
    return marked.parse(text) as string;
  }

  toggleSort(column: 'occurredAt' | 'durationMillis'): void {
    if (this.sortColumn === column) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortColumn = column;
      this.sortDirection = 'desc';
    }
    this.sortQueries();
    this.cdr.markForCheck();
  }

  private sortQueries(): void {
    this.sortedQueries = [...this.queries].sort((a, b) => {
      let cmp: number;
      if (this.sortColumn === 'occurredAt') {
        cmp = new Date(a.occurredAt).getTime() - new Date(b.occurredAt).getTime();
      } else {
        cmp = a.durationMillis - b.durationMillis;
      }
      return this.sortDirection === 'asc' ? cmp : -cmp;
    });
  }

  sortIcon(column: 'occurredAt' | 'durationMillis'): string {
    if (this.sortColumn !== column) return '';
    return this.sortDirection === 'asc' ? ' ▲' : ' ▼';
  }

  formatDate(dateStr: string): string {
    return new Intl.DateTimeFormat(this.locale, { dateStyle: 'short', timeStyle: 'medium' }).format(
      new Date(dateStr)
    );
  }

  onLocaleChange(): void {
    if (this.queries.length) {
      this.buildChart(this.queries);
      this.cdr.markForCheck();
    }
  }

  private toDatetimeLocal(date: Date): string {
    const pad = (n: number) => String(n).padStart(2, '0');
    return (
      `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}` +
      `T${pad(date.getHours())}:${pad(date.getMinutes())}`
    );
  }
}

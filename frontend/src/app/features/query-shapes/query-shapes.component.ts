import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { marked } from 'marked';
import { SlowQueryService } from '../../services/slow-query.service';
import { QueryShapeStats } from '../../models/query-shape.model';
import { SlowQueryResponse, SlowQueryAnalysisResponse } from '../../models/slow-query.model';

@Component({
  selector: 'app-query-shapes',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './query-shapes.component.html',
  styleUrl: './query-shapes.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QueryShapesComponent implements OnInit {
  private readonly slowQueryService = inject(SlowQueryService);
  private readonly cdr = inject(ChangeDetectorRef);

  startDate = '';
  endDate = '';
  loading = false;
  error = '';
  shapes: QueryShapeStats[] = [];
  sortColumn: keyof QueryShapeStats = 'totalDurationMillis';
  sortDirection: 'asc' | 'desc' = 'desc';

  selectedShape: QueryShapeStats | null = null;
  activeTab: 'details' | 'analysis' = 'details';
  sampleQuery: SlowQueryResponse | null = null;
  loadingSample = false;
  databaseAnalysisHtml: string | null = null;
  analysisMetadata: SlowQueryAnalysisResponse | null = null;
  analyzing = false;
  loadingAnalysis = false;
  locale: 'en-US' | 'pt-BR' = 'pt-BR';

  ngOnInit(): void {
    const today = new Date();
    const start = new Date(today.getFullYear(), today.getMonth(), today.getDate() - 7, 0, 0);
    const end = new Date(today.getFullYear(), today.getMonth(), today.getDate(), 23, 59);
    this.startDate = this.toDatetimeLocal(start);
    this.endDate = this.toDatetimeLocal(end);
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.selectedShape = null;

    const startDate = new Date(this.startDate).toISOString();
    const endDate = new Date(this.endDate).toISOString();

    this.slowQueryService.getQueryShapeStats(startDate, endDate).subscribe({
      next: (data) => {
        this.shapes = data;
        this.sortShapes();
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err.message ?? 'Failed to load query shapes';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  toggleSort(column: keyof QueryShapeStats): void {
    if (this.sortColumn === column) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortColumn = column;
      this.sortDirection = 'desc';
    }
    this.sortShapes();
    this.cdr.markForCheck();
  }

  private sortShapes(): void {
    this.shapes = [...this.shapes].sort((a, b) => {
      const aVal = a[this.sortColumn] ?? 0;
      const bVal = b[this.sortColumn] ?? 0;
      const cmp = aVal < bVal ? -1 : aVal > bVal ? 1 : 0;
      return this.sortDirection === 'asc' ? cmp : -cmp;
    });
  }

  sortIcon(column: keyof QueryShapeStats): string {
    if (this.sortColumn !== column) return '';
    return this.sortDirection === 'asc' ? ' ▲' : ' ▼';
  }

  selectShape(shape: QueryShapeStats): void {
    this.selectedShape = shape;
    this.activeTab = 'details';
    this.sampleQuery = null;
    this.loadingSample = true;
    this.databaseAnalysisHtml = null;
    this.analysisMetadata = null;
    this.analyzing = false;
    this.loadingAnalysis = true;
    this.cdr.markForCheck();

    this.slowQueryService.getShapeSample(shape.shapeHash).subscribe({
      next: (sample) => {
        this.sampleQuery = sample;
        this.loadingSample = false;
        this.cdr.markForCheck();

        if (sample) {
          this.fetchAnalysis(sample);
        } else {
          this.loadingAnalysis = false;
          this.cdr.markForCheck();
        }
      },
      error: () => {
        this.loadingSample = false;
        this.loadingAnalysis = false;
        this.cdr.markForCheck();
      },
    });
  }

  private fetchAnalysis(query: SlowQueryResponse): void {
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
    this.selectedShape = null;
    this.sampleQuery = null;
    this.databaseAnalysisHtml = null;
    this.analysisMetadata = null;
    this.analyzing = false;
    this.loadingAnalysis = false;
    this.cdr.markForCheck();
  }

  analyzeQuery(): void {
    if (!this.sampleQuery || this.analyzing) return;
    this.analyzing = true;
    this.databaseAnalysisHtml = null;
    this.analysisMetadata = null;
    this.slowQueryService.analyzeQuery(this.sampleQuery, true).subscribe({
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

  formatDuration(ms: number): string {
    return (ms / 1000).toFixed(2) + 's';
  }

  formatDate(dateStr: string): string {
    return new Intl.DateTimeFormat(this.locale, { dateStyle: 'short', timeStyle: 'medium' }).format(
      new Date(dateStr)
    );
  }

  private toDatetimeLocal(date: Date): string {
    const pad = (n: number) => String(n).padStart(2, '0');
    return (
      `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}` +
      `T${pad(date.getHours())}:${pad(date.getMinutes())}`
    );
  }
}

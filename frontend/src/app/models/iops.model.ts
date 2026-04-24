export interface DataPoint {
  timestamp: string;
  value: number | null;
}

export interface PeakPoint {
  timestamp: string;
  value: number;
}

export interface MetricSummary {
  dataPoints: DataPoint[];
  peak: PeakPoint | null;
}

export interface IopsQueryResponse {
  processId: string;
  hostname: string;
  currentRole: string;
  partitionName: string;
  granularity: string;
  start: string;
  end: string;
  roleChanges: string[];
  read: MetricSummary;
  write: MetricSummary;
  total: MetricSummary;
  maxRead: MetricSummary;
  maxWrite: MetricSummary;
  maxTotal: MetricSummary;
}

export type Granularity = 'PT1M' | 'PT5M' | 'PT1H' | 'P1D';

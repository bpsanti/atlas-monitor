export interface QueryShapeStats {
  shapeHash: string;
  namespace: string;
  planSummary: string | null;
  normalizedFilter: string | null;
  queryCount: number;
  totalDurationMillis: number;
  avgDurationMillis: number;
  maxDurationMillis: number;
  minDurationMillis: number;
}

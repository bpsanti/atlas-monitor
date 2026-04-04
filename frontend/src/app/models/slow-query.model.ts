export interface QueryExecutionResponse {
  keysExaminedCount: number | null;
  docsExaminedCount: number | null;
  docsReturnedCount: number | null;
  hasIndexCoverage: boolean | null;
  hasSortStage: boolean | null;
  docsExaminedToReturnedRatio: number | null;
  keysExaminedToReturnedRatio: number | null;
  executionDurationMillis: number | null;
  responseLengthBytes: number | null;
  yieldsCount: number | null;
  remoteAddress: string | null;
  isCursorExhausted: boolean | null;
}

export interface SlowQueryResponse {
  occurredAt: string;
  operationType: string;
  namespace: string;
  durationMillis: number;
  planSummary: string | null;
  filter: string | null;
  execution: QueryExecutionResponse;
}

export interface SlowQueryAnalysisResponse {
  analysis: string;
  planSummary: string | null;
  analyzedAt: string;
  cached: boolean;
}

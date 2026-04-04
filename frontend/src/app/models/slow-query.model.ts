export interface SlowQueryResponse {
  date: string;
  type: string;
  namespace: string;
  durationMillis: number;
  planSummary: string | null;
  keysExamined: number | null;
  docsExamined: number | null;
  nreturned: number | null;
  docsExaminedReturnedRatio: number | null;
  keysExaminedReturnedRatio: number | null;
  hasIndexCoverage: boolean | null;
  hasSort: boolean | null;
  operationExecutionTime: number | null;
  responseLength: number | null;
  numYields: number | null;
  remote: string | null;
  cursorExhausted: boolean | null;
  filter: string | null;
}

export interface SlowQueryAnalysisResponse {
  analysis: string;
  planSummary: string | null;
  analyzedAt: string;
  cached: boolean;
}

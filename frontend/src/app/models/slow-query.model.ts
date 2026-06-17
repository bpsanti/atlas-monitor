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

export interface CodeAnalysisResponse {
  filePath: string;
  repositoryName: string;
  htmlUrl: string;
  lineNumber: number | null;
  analysis: string;
}

export interface SlowQueryAnalysisResponse {
  analysis: string;
  databaseAnalysis: string;
  codeAnalyses: CodeAnalysisResponse[];
  planSummary: string | null;
  analyzedAt: string;
  cached: boolean;
}

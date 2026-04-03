export interface SlowQueryResponse {
  date: string;
  type: string;
  namespace: string;
  durationMillis: number;
  planSummary: string | null;
  keysExamined: number | null;
  docsExamined: number | null;
  nreturned: number | null;
  remote: string | null;
  cursorExhausted: boolean | null;
  filter: string | null;
}

import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SlowQueryResponse } from '../models/slow-query.model';

@Injectable({ providedIn: 'root' })
export class SlowQueryService {
  private readonly http = inject(HttpClient);

  getSlowQueries(
    since?: string,
    durationMs?: number,
    minDurationMillis?: number,
    nLogs?: number
  ): Observable<SlowQueryResponse[]> {
    let params = new HttpParams();
    if (since) params = params.set('since', since);
    if (durationMs != null) params = params.set('durationMs', durationMs);
    if (minDurationMillis != null) params = params.set('minDurationMillis', minDurationMillis);
    if (nLogs != null) params = params.set('nLogs', nLogs);
    return this.http.get<SlowQueryResponse[]>('/api/v1/slow-queries', { params });
  }

  analyzeQuery(query: SlowQueryResponse): Observable<{ analysis: string }> {
    return this.http.post<{ analysis: string }>('/api/v1/slow-queries/analyze', query);
  }
}

import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { SlowQueryResponse, SlowQueryAnalysisResponse } from '../models/slow-query.model';

@Injectable({ providedIn: 'root' })
export class SlowQueryService {
  private readonly http = inject(HttpClient);

  getSlowQueries(
    startDate?: string,
    endDate?: string,
    minDurationMillis?: number,
    nLogs?: number
  ): Observable<SlowQueryResponse[]> {
    let params = new HttpParams();
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);
    if (minDurationMillis != null) params = params.set('minDurationMillis', minDurationMillis);
    if (nLogs != null) params = params.set('nLogs', nLogs);
    return this.http.get<SlowQueryResponse[]>('/api/v1/slow-queries', { params });
  }

  findAnalysis(query: SlowQueryResponse): Observable<SlowQueryAnalysisResponse | null> {
    return this.http.post('/api/v1/slow-queries/analysis', query, { observe: 'response' }).pipe(
      map(response => response.status === 204 ? null : response.body as SlowQueryAnalysisResponse)
    );
  }

  analyzeQuery(query: SlowQueryResponse): Observable<SlowQueryAnalysisResponse> {
    return this.http.post<SlowQueryAnalysisResponse>('/api/v1/slow-queries/analyze', query);
  }
}

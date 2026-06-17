import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { SlowQueryResponse, SlowQueryAnalysisResponse } from '../models/slow-query.model';
import { QueryShapeStats } from '../models/query-shape.model';

@Injectable({ providedIn: 'root' })
export class SlowQueryService {
  private readonly http = inject(HttpClient);

  getSlowQueries(
    startDate?: string,
    endDate?: string,
    minDurationMillis?: number
  ): Observable<SlowQueryResponse[]> {
    let params = new HttpParams();
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);
    if (minDurationMillis != null) params = params.set('minDurationMillis', minDurationMillis);
    return this.http.get<SlowQueryResponse[]>('/api/v1/slow-queries', { params });
  }

  getShapeSample(shapeHash: string): Observable<SlowQueryResponse | null> {
    return this.http.get(`/api/v1/slow-queries/shapes/${shapeHash}/sample`, { observe: 'response' }).pipe(
      map(response => response.status === 204 ? null : response.body as SlowQueryResponse)
    );
  }

  getQueryShapeStats(startDate: string, endDate: string): Observable<QueryShapeStats[]> {
    const params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate);
    return this.http.get<QueryShapeStats[]>('/api/v1/slow-queries/shapes', { params });
  }

  findAnalysis(query: SlowQueryResponse): Observable<SlowQueryAnalysisResponse | null> {
    return this.http.post('/api/v1/slow-queries/analysis', query, { observe: 'response' }).pipe(
      map(response => response.status === 204 ? null : response.body as SlowQueryAnalysisResponse)
    );
  }

  analyzeQuery(query: SlowQueryResponse, force = false): Observable<SlowQueryAnalysisResponse> {
    let params = new HttpParams();
    if (force) params = params.set('force', true);
    return this.http.post<SlowQueryAnalysisResponse>('/api/v1/slow-queries/analyze', query, { params });
  }
}

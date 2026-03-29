import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { IopsQueryResponse } from '../models/iops.model';

@Injectable({ providedIn: 'root' })
export class IopsService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/iops';

  queryIops(granularity: string, start: string, end: string): Observable<IopsQueryResponse[]> {
    const params = new HttpParams()
      .set('granularity', granularity)
      .set('start', start)
      .set('end', end);
    return this.http.get<IopsQueryResponse[]>(this.baseUrl, { params });
  }

  queryPrimaryIops(granularity: string, start: string, end: string): Observable<IopsQueryResponse> {
    const params = new HttpParams()
      .set('granularity', granularity)
      .set('start', start)
      .set('end', end);
    return this.http.get<IopsQueryResponse>(`${this.baseUrl}/primary`, { params });
  }
}

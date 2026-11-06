import { inject, Service } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  CommunityDetail,
  CommunitySummary,
  CreateCommunityRequest
} from '../models/community.model';

@Service()
export class CommunityService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/communities';

  findMine(): Observable<CommunitySummary[]> {
    return this.http.get<CommunitySummary[]>(this.apiUrl);
  }

  findById(id: number): Observable<CommunityDetail> {
    return this.http.get<CommunityDetail>(`${this.apiUrl}/${id}`);
  }

  create(request: CreateCommunityRequest): Observable<CommunitySummary> {
    return this.http.post<CommunitySummary>(this.apiUrl, request);
  }
}

import { HttpClient, HttpParams } from '@angular/common/http';
import { Service, inject } from '@angular/core';
import { CommunitySearchFilter, CommunitySearchPage } from '../models/community-search.model';

@Service()
export class CommunitySearchService {
  private readonly http = inject(HttpClient);

  search(groupId: number, query: string, kind: CommunitySearchFilter, page: number) {
    const params = new HttpParams().set('q', query).set('kind', kind).set('page', page);
    return this.http.get<CommunitySearchPage>(`/api/communities/${groupId}/search`, { params });
  }
}

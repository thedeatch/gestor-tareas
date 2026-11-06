import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';

export type ReportResolution = 'DISMISSED' | 'REMOVED';

export interface CommunityReport {
  id: number;
  postId: number;
  commentId: number | null;
  reason: string;
  createdAt: string;
  content: string | null;
}

@Service()
export class CommunityReportsService {
  private readonly http = inject(HttpClient);

  private url(groupId: number): string {
    return `/api/communities/${groupId}/reports`;
  }

  create(groupId: number, postId: number, commentId: number | undefined, reason: string) {
    return this.http.post<void>(this.url(groupId), { postId, commentId, reason });
  }

  pending(groupId: number) {
    return this.http.get<CommunityReport[]>(this.url(groupId));
  }

  resolve(groupId: number, id: number, resolution: ReportResolution, reason: string) {
    return this.http.patch<void>(`${this.url(groupId)}/${id}`, { resolution, reason });
  }
}
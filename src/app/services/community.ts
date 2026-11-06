import { inject, Service } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  CommunityDetail,
  CommunityAccess,
  CommunityDiscovery,
  CommunityMember,
  CommunityRole,
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

  discover(): Observable<CommunityDiscovery[]> {
    return this.http.get<CommunityDiscovery[]>(`${this.apiUrl}/discover`);
  }

  requestToJoin(id: number): Observable<CommunityAccess> {
    return this.http.post<CommunityAccess>(
      `${this.apiUrl}/${id}/join-requests`, {}
    );
  }

  cancelJoinRequest(id: number): Observable<CommunityAccess> {
    return this.http.delete<CommunityAccess>(
      `${this.apiUrl}/${id}/join-requests/mine`
    );
  }

  findInvitations(): Observable<CommunityAccess[]> {
    return this.http.get<CommunityAccess[]>(`${this.apiUrl}/invitations`);
  }

  resolveInvitation(id: number, accept: boolean): Observable<CommunityAccess> {
    const action = accept ? 'accept' : 'reject';
    return this.http.patch<CommunityAccess>(
      `${this.apiUrl}/invitations/${id}/${action}`, {}
    );
  }

  invite(groupId: number, email: string): Observable<CommunityAccess> {
    return this.http.post<CommunityAccess>(
      `${this.apiUrl}/${groupId}/invitations`, { email }
    );
  }

  findJoinRequests(groupId: number): Observable<CommunityAccess[]> {
    return this.http.get<CommunityAccess[]>(
      `${this.apiUrl}/${groupId}/join-requests`
    );
  }

  resolveJoinRequest(
    groupId: number,
    requestId: number,
    approve: boolean
  ): Observable<CommunityAccess> {
    const action = approve ? 'approve' : 'reject';
    return this.http.patch<CommunityAccess>(
      `${this.apiUrl}/${groupId}/join-requests/${requestId}/${action}`, {}
    );
  }

  updateMemberRole(
    groupId: number,
    userId: number,
    role: CommunityRole
  ): Observable<CommunityMember> {
    return this.http.patch<CommunityMember>(
      `${this.apiUrl}/${groupId}/members/${userId}/role`, { role }
    );
  }
}

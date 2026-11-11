import { inject, Service } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  CommunityDetail,
  CommunityAccess,
  CommunityDiscovery,
  CommunityMember,
  CommunityPost,
  CommunityPostReaction,
  CommunityPostReactionType,
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

  getImage(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${id}/image`, { responseType: 'blob' });
  }

  updateImage(id: number, file: File): Observable<void> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<void>(`${this.apiUrl}/${id}/image`, formData);
  }

  removeImage(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}/image`);
  }

  updateGuidelines(groupId: number, extendedDescription: string, rules: string): Observable<CommunityDetail> {
    return this.http.patch<CommunityDetail>(`${this.apiUrl}/${groupId}/guidelines`, {
      extendedDescription, rules
    });
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

  findPosts(groupId: number): Observable<CommunityPost[]> {
    return this.http.get<CommunityPost[]>(
      `${this.apiUrl}/${groupId}/posts`
    );
  }

  createPost(
    groupId: number,
    title: string | null,
    content: string
  ): Observable<CommunityPost> {
    return this.http.post<CommunityPost>(
      `${this.apiUrl}/${groupId}/posts`, { title, content }
    );
  }

  updatePost(
    groupId: number,
    postId: number,
    title: string | null,
    content: string
  ): Observable<CommunityPost> {
    return this.http.patch<CommunityPost>(
      `${this.apiUrl}/${groupId}/posts/${postId}`, { title, content }
    );
  }

  setPostReaction(groupId: number, postId: number, type: CommunityPostReactionType,
    selected: boolean): Observable<CommunityPostReaction[]> {
    const url = `${this.apiUrl}/${groupId}/posts/${postId}/reactions/${type}`;
    return selected
      ? this.http.put<CommunityPostReaction[]>(url, {})
      : this.http.delete<CommunityPostReaction[]>(url);
  }

  deletePost(groupId: number, postId: number, reason?: string): Observable<void> {
    return this.http.delete<void>(
      `${this.apiUrl}/${groupId}/posts/${postId}`,
      { body: reason ? { reason } : undefined }
    );
  }
}

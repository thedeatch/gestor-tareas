import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable } from 'rxjs';

import {
  AdminAuditEntry,
  AdminUser,
  PageResponse
} from '../models/admin.model';
import { UserRole } from '../models/auth.model';

@Service()
export class AdminService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/admin/users';

  findUsers(
    search: string,
    page: number,
    size = 20
  ): Observable<PageResponse<AdminUser>> {
    const params = new HttpParams()
      .set('search', search)
      .set('page', page)
      .set('size', size);

    return this.http.get<PageResponse<AdminUser>>(this.apiUrl, { params });
  }

  updateRole(
    userId: number,
    role: UserRole,
    reason: string
  ): Observable<AdminUser> {
    return this.http.patch<AdminUser>(
      `${this.apiUrl}/${userId}/role`,
      { role, reason }
    );
  }

  updateStatus(
    userId: number,
    active: boolean,
    reason: string
  ): Observable<AdminUser> {
    return this.http.patch<AdminUser>(
      `${this.apiUrl}/${userId}/status`,
      { active, reason }
    );
  }

  findAuditLog(
    page: number,
    filters: {
      search?: string;
      action?: string;
      from?: string;
      to?: string;
    } = {},
    size = 20
  ): Observable<PageResponse<AdminAuditEntry>> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size);

    if (filters.search) params = params.set('search', filters.search);
    if (filters.action) params = params.set('action', filters.action);
    if (filters.from) params = params.set('from', filters.from);
    if (filters.to) params = params.set('to', filters.to);

    return this.http.get<PageResponse<AdminAuditEntry>>(
      '/api/admin/audit',
      { params }
    );
  }
}

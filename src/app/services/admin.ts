import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable } from 'rxjs';

import { AdminUser, PageResponse } from '../models/admin.model';
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

  updateRole(userId: number, role: UserRole): Observable<AdminUser> {
    return this.http.patch<AdminUser>(
      `${this.apiUrl}/${userId}/role`,
      { role }
    );
  }

  updateStatus(userId: number, active: boolean): Observable<AdminUser> {
    return this.http.patch<AdminUser>(
      `${this.apiUrl}/${userId}/status`,
      { active }
    );
  }
}

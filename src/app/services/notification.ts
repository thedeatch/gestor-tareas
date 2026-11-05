import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Service, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import {
  NotificationPage,
  UserNotification
} from '../models/notification.model';

@Service()
export class NotificationService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/notifications';
  private readonly unreadCountState = signal(0);

  readonly unreadCount = this.unreadCountState.asReadonly();

  findAll(
    page: number,
    unreadOnly: boolean,
    size = 20
  ): Observable<NotificationPage> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('unreadOnly', unreadOnly);
    return this.http.get<NotificationPage>(this.apiUrl, { params });
  }

  refreshUnreadCount(): void {
    this.http.get<{ count: number }>(`${this.apiUrl}/unread-count`)
      .subscribe({
        next: response => this.unreadCountState.set(response.count),
        error: () => this.unreadCountState.set(0)
      });
  }

  markAsRead(notification: UserNotification): Observable<UserNotification> {
    return this.http.patch<UserNotification>(
      `${this.apiUrl}/${notification.id}/read`,
      {}
    ).pipe(
      tap(() => {
        if (!notification.readAt) {
          this.unreadCountState.update(count => Math.max(0, count - 1));
        }
      })
    );
  }

  markAllAsRead(): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/read-all`, {}).pipe(
      tap(() => this.unreadCountState.set(0))
    );
  }

  clearCount(): void {
    this.unreadCountState.set(0);
  }
}

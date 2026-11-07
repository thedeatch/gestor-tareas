import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { inject, Service, signal } from '@angular/core';
import { defer, Observable, repeat, retry, Subscription, throwError, timer } from 'rxjs';

import {
  NotificationPage,
  UserNotification
} from '../models/notification.model';

interface LiveState {
  latestId: number;
  count: number;
}

@Service()
export class NotificationService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/notifications';
  private readonly unreadCountState = signal(0);

  private readonly revisionState = signal(0);
  readonly revision = this.revisionState.asReadonly();

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

  watch(): Subscription {
    let state: LiveState | null = null;
    return defer(() => {
      const params = state
        ? new HttpParams().set('latestId', state.latestId).set('count', state.count)
        : new HttpParams();
      return this.http.get<LiveState | null>(`${this.apiUrl}/live`, { params });
    }).pipe(
      retry({ delay: error => error instanceof HttpErrorResponse && [401, 403].includes(error.status)
        ? throwError(() => error) : timer(3000) }),
      repeat()
    ).subscribe({
      next: update => {
        if (!update) return;
        state = update;
        this.unreadCountState.set(update.count);
        this.revisionState.update(value => value + 1);
      },
      error: () => this.clearCount()
    });
  }

  markAsRead(notification: UserNotification): Observable<UserNotification> {
    return this.http.patch<UserNotification>(
      `${this.apiUrl}/${notification.id}/read`,
      {}
    );
  }

  markAllAsRead(): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/read-all`, {});
  }

  clearCount(): void {
    this.unreadCountState.set(0);
  }
}

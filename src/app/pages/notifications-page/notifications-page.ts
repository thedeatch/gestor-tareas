import { CommonModule } from '@angular/common';
import { Component, DestroyRef, effect, inject, signal, untracked } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Router } from '@angular/router';
import { finalize, Subscription } from 'rxjs';

import {
  NotificationType,
  UserNotification
} from '../../models/notification.model';
import { NotificationService } from '../../services/notification';

@Component({
  selector: 'app-notifications-page',
  imports: [
    CommonModule,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './notifications-page.html',
  styleUrl: './notifications-page.css'
})
export class NotificationsPage {
  private readonly notificationService = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);
  private request?: Subscription;
  private requestedPage = 0;
  private readonly router = inject(Router);

  protected readonly notifications = signal<UserNotification[]>([]);
  protected readonly page = signal(0);
  protected readonly totalPages = signal(0);
  protected readonly totalElements = signal(0);
  protected readonly unreadOnly = signal(false);
  protected readonly isLoading = signal(false);
  protected readonly isMarkingAll = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly unreadCount = this.notificationService.unreadCount;

  constructor() {
    effect(() => {
      this.notificationService.revision();
      untracked(() => this.loadNotifications(this.requestedPage));
    });
  }

  protected setUnreadOnly(value: boolean): void {
    if (this.unreadOnly() === value) return;
    this.unreadOnly.set(value);
    this.loadNotifications(0);
  }

  protected openNotification(notification: UserNotification): void {
    const navigate = () => {
      if (notification.targetUrl) {
        void this.router.navigateByUrl(notification.targetUrl);
      }
    };
    if (notification.readAt) {
      navigate();
      return;
    }
    this.notificationService.markAsRead(notification).subscribe({
      next: updated => {
        this.replaceNotification(updated);
        navigate();
      },
      error: () => this.errorMessage.set(
        'No fue posible actualizar la notificación.'
      )
    });
  }

  protected markAsRead(notification: UserNotification): void {
    this.notificationService.markAsRead(notification).subscribe({
      next: updated => this.replaceNotification(updated),
      error: () => this.errorMessage.set(
        'No fue posible actualizar la notificación.'
      )
    });
  }

  protected markAllAsRead(): void {
    if (this.isMarkingAll() || this.notificationService.unreadCount() === 0) {
      return;
    }
    this.isMarkingAll.set(true);
    this.notificationService.markAllAsRead().pipe(
      finalize(() => this.isMarkingAll.set(false))
    ).subscribe({
      next: () => this.loadNotifications(this.page()),
      error: () => this.errorMessage.set(
        'No fue posible marcar las notificaciones como leídas.'
      )
    });
  }

  protected previousPage(): void {
    if (this.page() > 0) this.loadNotifications(this.page() - 1);
  }

  protected nextPage(): void {
    if (this.page() + 1 < this.totalPages()) {
      this.loadNotifications(this.page() + 1);
    }
  }

  protected iconFor(type: NotificationType): string {
    return {
      SYSTEM: 'info',
      INVITATION: 'group_add',
      TASK: 'task_alt',
      COMMENT: 'comment',
      REACTION: 'favorite',
      EVENT: 'event',
      MODERATION: 'admin_panel_settings'
    }[type];
  }

  private loadNotifications(page = 0): void {
    this.requestedPage = page;
    this.request?.unsubscribe();
    this.errorMessage.set(null);
    this.isLoading.set(true);
    this.request = this.notificationService.findAll(page, this.unreadOnly()).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.isLoading.set(false))
    ).subscribe({
      next: response => {
        this.notifications.set(response.content);
        this.page.set(response.number);
        this.totalPages.set(response.totalPages);
        this.totalElements.set(response.totalElements);
        if (page > 0 && page >= response.totalPages) {
          queueMicrotask(() => {
            if (!this.destroyRef.destroyed && this.requestedPage === page) {
              this.loadNotifications(Math.max(0, response.totalPages - 1));
            }
          });
        }
      },
      error: () => this.errorMessage.set(
        'No fue posible cargar las notificaciones.'
      )
    });
  }

  private replaceNotification(updated: UserNotification): void {
    if (this.unreadOnly()) {
      this.loadNotifications(this.page());
      return;
    }
    this.notifications.update(items => items.map(item =>
      item.id === updated.id ? updated : item
    ));
  }
}

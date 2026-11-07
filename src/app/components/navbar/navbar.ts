import { DatePipe } from '@angular/common';
import {
  Component,
  computed,
  effect,
  inject,
  signal,
  untracked,
  viewChild
} from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatBadgeModule } from '@angular/material/badge';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule, MatMenuTrigger } from '@angular/material/menu';
import { MatToolbarModule } from '@angular/material/toolbar';
import {
  Router,
  RouterLink,
  RouterLinkActive
} from '@angular/router';
import { AuthService } from '../../services/auth';
import { ThemePreference } from '../../models/user-preferences.model';
import { UserPreferencesService } from '../../services/user-preferences';
import { APP_MODULES } from '../../models/app-module.model';
import { ModuleConfigurationService } from '../../services/module-configuration';
import { NotificationService } from '../../services/notification';
import { NotificationType, UserNotification } from '../../models/notification.model';
import { finalize, Subscription } from 'rxjs';
@Component({
  selector: 'app-navbar',
  imports: [
  DatePipe,
  MatBadgeModule,
  MatButtonModule,
  MatIconModule,
  MatMenuModule,
  MatToolbarModule,
  RouterLink,
  RouterLinkActive
],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css'
})
export class Navbar {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly preferencesService = inject(UserPreferencesService);
  private readonly moduleConfiguration = inject(ModuleConfigurationService);
  private readonly notificationService = inject(NotificationService);
  private previewRequest?: Subscription;
  private readonly notificationMenuTrigger =
    viewChild<MatMenuTrigger>('notificationTrigger');

  protected readonly isAuthenticated =
    this.authService.isAuthenticated;

  protected readonly userName =
    this.authService.userName;

  protected readonly userRole =
    this.authService.userRole;

  protected readonly avatarUrl = signal<string | null>(null);
  protected readonly isChangingTheme = signal(false);
  protected readonly preferences = this.preferencesService.preferences;
  protected readonly unreadNotifications = this.notificationService.unreadCount;
  protected readonly notificationPreview = signal<UserNotification[]>([]);
  protected readonly isLoadingNotifications = signal(false);
  protected readonly isMarkingNotifications = signal(false);
  protected readonly notificationError = signal<string | null>(null);
  protected readonly navigationModules = computed(() => {
    const role = this.userRole();
    return APP_MODULES.filter(module =>
      module.id !== 'profile' &&
      this.moduleConfiguration.isEnabled(module.id) &&
      (!module.roles || (role !== null && module.roles.includes(role)))
    );
  });

  constructor() {
    this.moduleConfiguration.load().subscribe({ error: () => undefined });
    effect(() => {
      this.authService.avatarRevision();
      if (this.isAuthenticated()) {
        this.loadAvatar();
      } else {
        this.replaceAvatarUrl(null);
      }
    });
    effect(onCleanup => {
      const account = this.authService.userEmail();
      const authenticated = this.isAuthenticated();
      untracked(() => {
        this.previewRequest?.unsubscribe();
        this.notificationPreview.set([]);
        this.notificationService.clearCount();
        if (account && authenticated) {
          const connection = this.notificationService.watch();
          onCleanup(() => {
            connection.unsubscribe();
            this.previewRequest?.unsubscribe();
          });
        }
      });
    });
    effect(() => {
      this.notificationService.revision();
      untracked(() => {
        if (this.notificationMenuTrigger()?.menuOpen) this.loadNotificationPreview();
      });
    });
  }

  protected logout(): void {
    this.authService.logoutFromServer().subscribe({
      complete: () => void this.router.navigateByUrl('/login'),
      error: () => void this.router.navigateByUrl('/login')
    });
  }

  protected changeTheme(): void {
    if (this.isChangingTheme()) {
      return;
    }

    const current = this.preferences();
    this.isChangingTheme.set(true);
    this.preferencesService.update({
      ...current,
      theme: this.nextTheme(current.theme)
    }).subscribe({
      error: () => this.isChangingTheme.set(false),
      complete: () => this.isChangingTheme.set(false)
    });
  }

  protected loadNotificationPreview(): void {
    this.previewRequest?.unsubscribe();
    this.notificationError.set(null);
    this.isLoadingNotifications.set(true);
    this.previewRequest = this.notificationService.findAll(0, false, 6).pipe(
      finalize(() => this.isLoadingNotifications.set(false))
    ).subscribe({
      next: response => this.notificationPreview.set(response.content),
      error: () => this.notificationError.set(
        'No fue posible cargar las notificaciones.'
      )
    });
  }

  protected openNotification(notification: UserNotification): void {
    const navigate = () => {
      this.closeNotificationMenu();
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
        this.notificationPreview.update(items => items.map(item =>
          item.id === updated.id ? updated : item
        ));
        navigate();
      },
      error: () => this.notificationError.set(
        'No fue posible actualizar la notificación.'
      )
    });
  }

  protected closeNotificationMenu(): void {
    this.notificationMenuTrigger()?.closeMenu();
  }

  protected markAllNotificationsAsRead(event: Event): void {
    event.stopPropagation();
    if (this.isMarkingNotifications() || this.unreadNotifications() === 0) {
      return;
    }
    this.isMarkingNotifications.set(true);
    this.notificationService.markAllAsRead().pipe(
      finalize(() => this.isMarkingNotifications.set(false))
    ).subscribe({
      next: () => this.notificationPreview.update(items => items.map(item => ({
        ...item,
        readAt: item.readAt ?? new Date().toISOString()
      }))),
      error: () => this.notificationError.set(
        'No fue posible marcar las notificaciones como leídas.'
      )
    });
  }

  protected notificationIcon(type: NotificationType): string {
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

  protected themeIcon(): string {
    return this.preferences().theme === 'DARK'
      ? 'dark_mode'
      : 'light_mode';
  }

  protected themeTooltip(): string {
    const names: Record<ThemePreference, string> = {
      LIGHT: 'Claro',
      DARK: 'Oscuro'
    };
    const current = this.preferences().theme;
    return `Tema actual: ${names[current]}. Cambiar a ${names[this.nextTheme(current)]}`;
  }

  private loadAvatar(): void {
    this.authService.getAvatar().subscribe({
      next: blob => this.replaceAvatarUrl(URL.createObjectURL(blob)),
      error: () => this.replaceAvatarUrl(null)
    });
  }

  private replaceAvatarUrl(url: string | null): void {
    const previousUrl = this.avatarUrl();
    if (previousUrl) {
      URL.revokeObjectURL(previousUrl);
    }
    this.avatarUrl.set(url);
  }

  private nextTheme(theme: ThemePreference): ThemePreference {
    return theme === 'LIGHT' ? 'DARK' : 'LIGHT';
  }
}

import { Component, computed, effect, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
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
@Component({
  selector: 'app-navbar',
  imports: [
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

  protected readonly isAuthenticated =
    this.authService.isAuthenticated;

  protected readonly userName =
    this.authService.userName;

  protected readonly userRole =
    this.authService.userRole;

  protected readonly avatarUrl = signal<string | null>(null);
  protected readonly isChangingTheme = signal(false);
  protected readonly preferences = this.preferencesService.preferences;
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

import { Component, effect, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatToolbarModule } from '@angular/material/toolbar';
import {
  Router,
  RouterLink,
  RouterLinkActive
} from '@angular/router';
import { AuthService } from '../../services/auth';
@Component({
  selector: 'app-navbar',
  imports: [
  MatButtonModule,
  MatIconModule,
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

  protected readonly isAuthenticated =
    this.authService.isAuthenticated;

  protected readonly userName =
    this.authService.userName;

  protected readonly userRole =
    this.authService.userRole;

  protected readonly avatarUrl = signal<string | null>(null);

  constructor() {
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
    this.authService.logout();
    void this.router.navigateByUrl('/login');
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
}

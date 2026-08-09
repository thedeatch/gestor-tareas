import { Component, inject } from '@angular/core';
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

  protected readonly userEmail =
    this.authService.userEmail;

  protected logout(): void {
    this.authService.logout();
    void this.router.navigateByUrl('/login');
  }
}
import { Component, computed, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';

import { APP_MODULES } from '../../models/app-module.model';
import { AuthService } from '../../services/auth';

@Component({
  selector: 'app-dashboard-page',
  imports: [
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    RouterLink
  ],
  templateUrl: './dashboard-page.html',
  styleUrl: './dashboard-page.css'
})
export class DashboardPage {
  private readonly authService = inject(AuthService);

  protected readonly userName = this.authService.userName;
  protected readonly modules = computed(() => {
    const role = this.authService.userRole();
    return APP_MODULES.filter(module =>
      !module.roles || (role !== null && module.roles.includes(role))
    );
  });
}

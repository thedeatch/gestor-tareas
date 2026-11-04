import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { AuthService } from '../../services/auth';

@Component({
  selector: 'app-verify-email-page',
  imports: [
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule,
    RouterLink
  ],
  templateUrl: './verify-email-page.html',
  styleUrl: '../login-page/login-page.css'
})
export class VerifyEmailPage {
  private readonly authService = inject(AuthService);
  private readonly route = inject(ActivatedRoute);

  protected readonly isLoading = signal(true);
  protected readonly verified = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  constructor() {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) {
      this.isLoading.set(false);
      this.errorMessage.set('El enlace de verificación está incompleto.');
      return;
    }

    this.authService.verifyEmail(token).pipe(
      finalize(() => this.isLoading.set(false))
    ).subscribe({
      next: () => this.verified.set(true),
      error: () => this.errorMessage.set(
        'El enlace de verificación no es válido o ya expiró.'
      )
    });
  }
}

import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { AuthService } from '../../services/auth';

@Component({
  selector: 'app-forgot-password-page',
  imports: [
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    RouterLink
  ],
  templateUrl: './forgot-password-page.html',
  styleUrl: '../login-page/login-page.css'
})
export class ForgotPasswordPage {
  private readonly authService = inject(AuthService);

  protected email = '';
  protected readonly isSubmitting = signal(false);
  protected readonly requestCompleted = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected requestReset(): void {
    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    this.authService.forgotPassword(this.email)
      .pipe(finalize(() => this.isSubmitting.set(false)))
      .subscribe({
        next: () => this.requestCompleted.set(true),
        error: () => this.errorMessage.set(
          'No fue posible procesar la solicitud. Inténtalo nuevamente.'
        )
      });
  }
}


import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { AuthService } from '../../services/auth';

@Component({
  selector: 'app-reset-password-page',
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
  templateUrl: './reset-password-page.html',
  styleUrl: '../login-page/login-page.css'
})
export class ResetPasswordPage {
  private readonly authService = inject(AuthService);
  private readonly route = inject(ActivatedRoute);

  protected readonly token =
    this.route.snapshot.queryParamMap.get('token') ?? '';
  protected password = '';
  protected passwordConfirmation = '';
  protected readonly hidePassword = signal(true);
  protected readonly isSubmitting = signal(false);
  protected readonly resetCompleted = signal(false);
  protected readonly errorMessage = signal<string | null>(
    this.token ? null : 'El enlace de recuperación no es válido.'
  );

  protected togglePasswordVisibility(): void {
    this.hidePassword.update(hidden => !hidden);
  }

  protected passwordsMatch(): boolean {
    return this.password === this.passwordConfirmation;
  }

  protected resetPassword(): void {
    if (!this.token || !this.passwordsMatch()) {
      this.errorMessage.set(
        this.token
          ? 'Las contraseñas no coinciden.'
          : 'El enlace de recuperación no es válido.'
      );
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    this.authService.resetPassword(this.token, this.password)
      .pipe(finalize(() => this.isSubmitting.set(false)))
      .subscribe({
        next: () => this.resetCompleted.set(true),
        error: error => this.errorMessage.set(
          error.status === 400
            ? 'El enlace no es válido, ya fue utilizado o venció.'
            : 'No fue posible cambiar la contraseña. Inténtalo nuevamente.'
        )
      });
  }
}


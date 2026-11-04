import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { AuthService } from '../../services/auth';
import { getApiErrorMessage } from '../../models/api-error.model';
@Component({
  selector: 'app-login-page',
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
  templateUrl: './login-page.html',
  styleUrl: './login-page.css'
})
export class LoginPage {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  protected readonly credentials = {
    email: '',
    password: ''
  };

  protected readonly hidePassword = signal(true);
  protected readonly isSubmitting = signal(false);
  protected readonly isResending = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly verificationPending = signal(
    this.route.snapshot.queryParamMap.get('registration') ===
      'pending-verification'
  );
  protected readonly successMessage = signal<string | null>(this.initialMessage());

  protected togglePasswordVisibility(): void {
    this.hidePassword.update(hidden => !hidden);
  }

  protected login(): void {
    this.submit('login');
  }

  protected resendVerification(): void {
    if (!this.credentials.email || this.isResending()) {
      this.errorMessage.set(
        'Ingresa tu correo para solicitar un nuevo enlace.'
      );
      return;
    }

    this.isResending.set(true);
    this.errorMessage.set(null);
    this.authService.resendVerification(this.credentials.email).pipe(
      finalize(() => this.isResending.set(false))
    ).subscribe({
      next: () => this.successMessage.set(
        'Si la cuenta está pendiente, recibirás un nuevo enlace. Revisa también spam.'
      ),
      error: () => this.errorMessage.set(
        'No fue posible solicitar el correo. Inténtalo nuevamente.'
      )
    });
  }

  private submit(action: 'login'): void {
    this.isSubmitting.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.authService[action](this.credentials)
      .pipe(finalize(() => this.isSubmitting.set(false)))
      .subscribe({
        next: () => {
          void this.router.navigateByUrl('/tareas');
        },
        error: error => {
          const suspended = error.status === 403 &&
            getApiErrorMessage(error, '').toLowerCase()
              .includes('suspendida');
          this.verificationPending.set(
            error.status === 403 && !suspended
          );
          this.errorMessage.set(
            error.status === 429
              ? 'Demasiados intentos fallidos. Espera 15 minutos o recupera tu contraseña.'
              : error.status === 401
              ? 'Correo o contraseña incorrectos.'
              : error.status === 403
                ? suspended
                  ? 'Tu cuenta está suspendida. Contacta a un administrador.'
                  : 'Debes confirmar tu correo antes de iniciar sesión.'
              : 'No fue posible iniciar sesión. Inténtalo nuevamente.'
          );
        }
      });
  }

  private initialMessage(): string | null {
    if (this.router.getCurrentNavigation()?.extras.state?.['passwordChanged']) {
      return 'Contraseña actualizada. Inicia sesión nuevamente.';
    }
    if (this.route.snapshot.queryParamMap.get('session') === 'expired') {
      return 'Tu sesión expiró. Inicia sesión nuevamente para continuar.';
    }
    const registration = this.route.snapshot.queryParamMap.get('registration');
    if (registration === 'pending-verification') {
      return 'Cuenta creada. Revisa tu correo para confirmar la dirección.';
    }
    return registration === 'ready'
      ? 'Cuenta de prueba creada. Ya puedes iniciar sesión.'
      : null;
  }
}

import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { AuthService } from '../../services/auth';

@Component({
  selector: 'app-register-page',
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
  templateUrl: './register-page.html',
  styleUrl: '../login-page/login-page.css'
})
export class RegisterPage {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly registration = {
    name: '',
    email: '',
    password: '',
    passwordConfirmation: ''
  };

  protected readonly hidePassword = signal(true);
  protected readonly isSubmitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly emailAlreadyRegistered = signal(false);

  protected togglePasswordVisibility(): void {
    this.hidePassword.update(hidden => !hidden);
  }

  protected passwordsMatch(): boolean {
    return this.registration.password ===
      this.registration.passwordConfirmation;
  }

  protected register(): void {
    if (!this.passwordsMatch()) {
      this.errorMessage.set('Las contraseñas no coinciden.');
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set(null);
    this.emailAlreadyRegistered.set(false);

    this.authService.register({
      name: this.registration.name,
      email: this.registration.email,
      password: this.registration.password
    }).pipe(
      finalize(() => this.isSubmitting.set(false))
    ).subscribe({
      next: () => {
        void this.router.navigateByUrl('/tareas');
      },
      error: error => {
        const emailExists = error.status === 409;
        this.emailAlreadyRegistered.set(emailExists);
        this.errorMessage.set(
          emailExists
            ? 'Ya existe una cuenta con este correo. Intenta iniciar sesión.'
            : error.status === 400
              ? 'Revisa los datos ingresados e inténtalo nuevamente.'
              : 'No fue posible crear la cuenta. Inténtalo nuevamente.'
        );
      }
    });
  }
}

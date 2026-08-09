import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { Router } from '@angular/router';

import { AuthService } from '../../services/auth';
@Component({
  selector: 'app-login-page',
  imports: [
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule
  ],
  templateUrl: './login-page.html',
  styleUrl: './login-page.css'
})
export class LoginPage {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  protected readonly credentials = {
    email: '',
    password: ''
  };

  protected readonly hidePassword = signal(true);

  protected togglePasswordVisibility(): void {
    this.hidePassword.update(hidden => !hidden);
  }

  protected login(): void {
    this.authService.login(this.credentials.email);
    void this.router.navigateByUrl('/tareas');
  }
}
import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';

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
  protected readonly credentials = {
    email: '',
    password: ''
  };

  protected readonly hidePassword = signal(true);
  protected readonly message = signal('');

  protected togglePasswordVisibility(): void {
    this.hidePassword.update(hidden => !hidden);
  }

  protected login(): void {
    this.message.set(
      'Formulario válido. La autenticación real se conectará al backend.'
    );
  }
}
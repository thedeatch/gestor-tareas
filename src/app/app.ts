import { Component, effect, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { Navbar } from './components/navbar/navbar';
import { AuthService } from './services/auth';
import { UserPreferencesService } from './services/user-preferences';

@Component({
  selector: 'app-root',
  imports: [
    Navbar,
    RouterOutlet
  ],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  private readonly authService = inject(AuthService);
  private readonly preferencesService = inject(UserPreferencesService);

  constructor() {
    effect(() => {
      if (this.authService.isAuthenticated()) {
        this.preferencesService.load().subscribe({ error: () => undefined });
      } else {
        this.preferencesService.reset();
      }
    });
  }
}

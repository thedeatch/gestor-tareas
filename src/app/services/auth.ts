import { Service, signal } from '@angular/core';

@Service()
export class AuthService {
  private readonly storageKey = 'gestor-tareas.session';
  private readonly storedEmail = this.loadStoredEmail();

  private readonly authenticatedState = signal(
    this.storedEmail !== null
  );

  private readonly userEmailState = signal<string | null>(
    this.storedEmail
  );

  readonly isAuthenticated =
    this.authenticatedState.asReadonly();

  readonly userEmail =
    this.userEmailState.asReadonly();

  login(email: string): void {
    this.userEmailState.set(email);
    this.authenticatedState.set(true);

    if (typeof sessionStorage !== 'undefined') {
      sessionStorage.setItem(this.storageKey, email);
    }
  }

  logout(): void {
    this.userEmailState.set(null);
    this.authenticatedState.set(false);

    if (typeof sessionStorage !== 'undefined') {
      sessionStorage.removeItem(this.storageKey);
    }
  }

  private loadStoredEmail(): string | null {
    if (typeof sessionStorage === 'undefined') {
      return null;
    }

    return sessionStorage.getItem(this.storageKey);
  }
}
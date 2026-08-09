import { Service, signal } from '@angular/core';

@Service()
export class AuthService {
  private readonly authenticatedState = signal(false);
  private readonly userEmailState = signal<string | null>(null);

  readonly isAuthenticated = this.authenticatedState.asReadonly();
  readonly userEmail = this.userEmailState.asReadonly();

  login(email: string): void {
    this.userEmailState.set(email);
    this.authenticatedState.set(true);
  }

  logout(): void {
    this.userEmailState.set(null);
    this.authenticatedState.set(false);
  }
}
import { inject, Service, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, tap } from 'rxjs';

import {
  AuthCredentials,
  AuthResponse,
  RegistrationData
} from '../models/auth.model';

@Service()
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/auth';
  private readonly tokenStorageKey = 'gestor-tareas.token';
  private readonly emailStorageKey = 'gestor-tareas.email';
  private readonly nameStorageKey = 'gestor-tareas.name';
  private readonly storedToken = this.loadStoredValue(
    this.tokenStorageKey
  );
  private readonly storedEmail = this.loadStoredValue(
    this.emailStorageKey
  );
  private readonly storedName = this.loadStoredValue(
    this.nameStorageKey
  );

  private readonly authenticatedState = signal(
    this.storedToken !== null && this.storedEmail !== null
  );

  private readonly userEmailState = signal<string | null>(
    this.storedEmail
  );

  private readonly userNameState = signal<string | null>(
    this.storedName ?? this.storedEmail
  );

  readonly isAuthenticated =
    this.authenticatedState.asReadonly();

  readonly userEmail =
    this.userEmailState.asReadonly();

  readonly userName =
    this.userNameState.asReadonly();

  login(credentials: AuthCredentials): Observable<void> {
    return this.authenticate('/login', credentials);
  }

  register(data: RegistrationData): Observable<void> {
    return this.authenticate('/register', data);
  }

  getToken(): string | null {
    return this.loadStoredValue(this.tokenStorageKey);
  }

  logout(): void {
    this.userEmailState.set(null);
    this.userNameState.set(null);
    this.authenticatedState.set(false);

    if (typeof sessionStorage !== 'undefined') {
      sessionStorage.removeItem(this.tokenStorageKey);
      sessionStorage.removeItem(this.emailStorageKey);
      sessionStorage.removeItem(this.nameStorageKey);
    }
  }

  private authenticate(
    endpoint: '/login' | '/register',
    data: AuthCredentials | RegistrationData
  ): Observable<void> {
    return this.http.post<AuthResponse>(
      `${this.apiUrl}${endpoint}`,
      data
    ).pipe(
      tap(response => this.storeSession(response)),
      map(() => undefined)
    );
  }

  private storeSession(response: AuthResponse): void {
    this.userEmailState.set(response.email);
    this.userNameState.set(response.name);
    this.authenticatedState.set(true);

    if (typeof sessionStorage !== 'undefined') {
      sessionStorage.setItem(this.tokenStorageKey, response.token);
      sessionStorage.setItem(this.emailStorageKey, response.email);
      sessionStorage.setItem(this.nameStorageKey, response.name);
    }
  }

  private loadStoredValue(key: string): string | null {
    if (typeof sessionStorage === 'undefined') {
      return null;
    }

    return sessionStorage.getItem(key);
  }
}

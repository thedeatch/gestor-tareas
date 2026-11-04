import { inject, Service, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, finalize, map, tap } from 'rxjs';

import {
  AuthCredentials,
  AuthResponse,
  PasswordChangeData,
  ProfileUpdateData,
  RegistrationData,
  UserProfile
} from '../models/auth.model';
import { UserRole } from '../models/auth.model';

@Service()
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/auth';
  private readonly profileApiUrl = '/api/users/me';
  private readonly tokenStorageKey = 'gestor-tareas.token';
  private readonly emailStorageKey = 'gestor-tareas.email';
  private readonly nameStorageKey = 'gestor-tareas.name';
  private readonly roleStorageKey = 'gestor-tareas.role';
  private readonly storedToken = this.loadStoredValue(
    this.tokenStorageKey
  );
  private readonly storedEmail = this.loadStoredValue(
    this.emailStorageKey
  );
  private readonly storedName = this.loadStoredValue(
    this.nameStorageKey
  );
  private readonly storedRole = this.loadStoredRole();

  private readonly authenticatedState = signal(
    this.storedToken !== null &&
      this.storedEmail !== null &&
      !this.isTokenExpired(this.storedToken)
  );

  private readonly userEmailState = signal<string | null>(
    this.storedEmail
  );

  private readonly userNameState = signal<string | null>(
    this.storedName ?? this.storedEmail
  );

  private readonly avatarRevisionState = signal(0);

  private readonly userRoleState = signal<UserRole | null>(
    this.storedRole
  );

  readonly isAuthenticated =
    this.authenticatedState.asReadonly();

  readonly userEmail =
    this.userEmailState.asReadonly();

  readonly userName =
    this.userNameState.asReadonly();

  readonly avatarRevision =
    this.avatarRevisionState.asReadonly();

  readonly userRole = this.userRoleState.asReadonly();

  hasAnyRole(...roles: UserRole[]): boolean {
    const currentRole = this.userRoleState();
    return currentRole !== null && roles.includes(currentRole);
  }

  login(credentials: AuthCredentials): Observable<void> {
    return this.authenticate('/login', credentials);
  }

  refreshSession(): Observable<void> {
    return this.http.post<AuthResponse>(
      `${this.apiUrl}/refresh`,
      {}
    ).pipe(
      tap(response => this.storeSession(response)),
      map(() => undefined)
    );
  }

  logoutFromServer(): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/logout`, {}).pipe(
      finalize(() => this.logout())
    );
  }

  register(data: RegistrationData): Observable<{ verificationRequired: boolean }> {
    return this.http.post<{ verificationRequired: boolean }>(
      `${this.apiUrl}/register`,
      data
    );
  }

  verifyEmail(token: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/verify-email`, { token });
  }

  resendVerification(email: string): Observable<void> {
    return this.http.post<void>(
      `${this.apiUrl}/resend-verification`,
      { email }
    );
  }

  forgotPassword(email: string): Observable<void> {
    return this.http.post<void>(
      `${this.apiUrl}/forgot-password`,
      { email }
    );
  }

  resetPassword(token: string, password: string): Observable<void> {
    return this.http.post<void>(
      `${this.apiUrl}/reset-password`,
      { token, password }
    );
  }

  getProfile(): Observable<UserProfile> {
    return this.http.get<UserProfile>(this.profileApiUrl).pipe(
      tap(profile => this.storeRole(profile.role))
    );
  }

  updateProfile(data: ProfileUpdateData): Observable<void> {
    return this.http.patch<AuthResponse>(this.profileApiUrl, data).pipe(
      tap(response => this.storeSession(response)),
      map(() => undefined)
    );
  }

  changePassword(data: PasswordChangeData): Observable<void> {
    return this.http.post<void>(`${this.profileApiUrl}/password`, data);
  }

  getAvatar(): Observable<Blob> {
    return this.http.get(`${this.profileApiUrl}/avatar`, {
      responseType: 'blob'
    });
  }

  updateAvatar(file: File): Observable<void> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<void>(`${this.profileApiUrl}/avatar`, formData).pipe(
      tap(() => this.avatarRevisionState.update(value => value + 1))
    );
  }

  removeAvatar(): Observable<void> {
    return this.http.delete<void>(`${this.profileApiUrl}/avatar`).pipe(
      tap(() => this.avatarRevisionState.update(value => value + 1))
    );
  }

  getToken(): string | null {
    return this.loadStoredValue(this.tokenStorageKey);
  }

  isTokenExpired(token = this.getToken()): boolean {
    if (!token) {
      return false;
    }

    try {
      const payloadPart = token.split('.')[1];
      if (!payloadPart) {
        return false;
      }

      const normalizedPayload = payloadPart
        .replace(/-/g, '+')
        .replace(/_/g, '/');
      const payload = JSON.parse(atob(normalizedPayload));

      return typeof payload.exp === 'number' &&
        payload.exp * 1000 <= Date.now();
    } catch {
      return false;
    }
  }

  expireSession(): boolean {
    const hadSession = this.getToken() !== null;
    this.logout();
    return hadSession;
  }

  logout(): void {
    this.userEmailState.set(null);
    this.userNameState.set(null);
    this.userRoleState.set(null);
    this.authenticatedState.set(false);
    this.avatarRevisionState.update(value => value + 1);

    if (typeof sessionStorage !== 'undefined') {
      sessionStorage.removeItem(this.tokenStorageKey);
      sessionStorage.removeItem(this.emailStorageKey);
      sessionStorage.removeItem(this.nameStorageKey);
      sessionStorage.removeItem(this.roleStorageKey);
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
    this.userRoleState.set(response.role);
    this.authenticatedState.set(true);
    this.avatarRevisionState.update(value => value + 1);

    if (typeof sessionStorage !== 'undefined') {
      sessionStorage.setItem(this.tokenStorageKey, response.token);
      sessionStorage.setItem(this.emailStorageKey, response.email);
      sessionStorage.setItem(this.nameStorageKey, response.name);
      sessionStorage.setItem(this.roleStorageKey, response.role);
    }
  }

  private loadStoredValue(key: string): string | null {
    if (typeof sessionStorage === 'undefined') {
      return null;
    }

    return sessionStorage.getItem(key);
  }

  private loadStoredRole(): UserRole | null {
    const role = this.loadStoredValue(this.roleStorageKey);
    return role === 'USER' || role === 'MODERATOR' || role === 'ADMIN'
      ? role
      : null;
  }

  private storeRole(role: UserRole): void {
    this.userRoleState.set(role);
    if (typeof sessionStorage !== 'undefined') {
      sessionStorage.setItem(this.roleStorageKey, role);
    }
  }
}

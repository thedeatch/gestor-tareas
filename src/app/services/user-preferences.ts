import { HttpClient } from '@angular/common/http';
import { inject, Service, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import { UserPreferences } from '../models/user-preferences.model';

const DEFAULT_PREFERENCES: UserPreferences = {
  theme: 'LIGHT',
  reducedMotion: false
};

@Service()
export class UserPreferencesService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/users/me/preferences';
  private readonly preferencesState = signal<UserPreferences>(
    DEFAULT_PREFERENCES
  );

  readonly preferences = this.preferencesState.asReadonly();

  load(): Observable<UserPreferences> {
    return this.http.get<UserPreferences>(this.apiUrl).pipe(
      tap(preferences => this.apply(preferences))
    );
  }

  update(preferences: UserPreferences): Observable<UserPreferences> {
    return this.http.put<UserPreferences>(this.apiUrl, preferences).pipe(
      tap(saved => this.apply(saved))
    );
  }

  reset(): void {
    this.apply(DEFAULT_PREFERENCES);
  }

  private apply(preferences: UserPreferences): void {
    this.preferencesState.set(preferences);
    if (typeof document === 'undefined') {
      return;
    }

    document.documentElement.dataset['theme'] =
      preferences.theme.toLowerCase();
    document.documentElement.classList.toggle(
      'reduced-motion',
      preferences.reducedMotion
    );
  }
}

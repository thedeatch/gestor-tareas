import { HttpClient } from '@angular/common/http';
import { Service, inject, signal } from '@angular/core';
import { Observable, of, tap } from 'rxjs';

import {
  ModuleAvailability,
  ModuleId
} from '../models/app-module.model';

@Service()
export class ModuleConfigurationService {
  private readonly http = inject(HttpClient);
  private readonly loaded = signal(false);
  private readonly availabilityState = signal<ModuleAvailability>({
    tasks: true,
    communities: true,
    profile: true,
    users: true,
    audit: true
  });

  readonly availability = this.availabilityState.asReadonly();

  load(): Observable<ModuleAvailability> {
    if (this.loaded()) {
      return of(this.availabilityState());
    }

    return this.http.get<ModuleAvailability>('/api/modules').pipe(
      tap(availability => {
        this.availabilityState.set(availability);
        this.loaded.set(true);
      })
    );
  }

  isEnabled(moduleId: ModuleId): boolean {
    return this.availabilityState()[moduleId];
  }
}

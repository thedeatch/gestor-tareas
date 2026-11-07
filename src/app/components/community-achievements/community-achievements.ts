import { DatePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, DestroyRef, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { finalize } from 'rxjs';

import { getApiErrorMessage } from '../../models/api-error.model';

interface Achievement {
  code: string;
  title: string;
  description: string;
  current: number;
  target: number;
  earnedAt: string | null;
}

@Component({
  selector: 'app-community-achievements',
  imports: [DatePipe, MatButtonModule, MatCardModule],
  templateUrl: './community-achievements.html',
  styles: [`
    :host { display: block; margin-bottom: 1rem; }
    li { margin-block: .75rem; }
    li p { margin-block: .25rem; }
    progress { max-width: 100%; margin-inline-end: .5rem; }
  `]
})
export class CommunityAchievements {
  readonly groupId = input.required<number>();
  private readonly http = inject(HttpClient);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly items = signal<Achievement[] | null>(null);
  protected readonly loading = signal(false);
  protected readonly error = signal('');

  protected load(): void {
    if (this.loading()) return;
    this.loading.set(true);
    this.error.set('');
    this.http.get<Achievement[]>(`/api/communities/${this.groupId()}/achievements/mine`)
      .pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.loading.set(false))).subscribe({
        next: items => this.items.set(items),
        error: error => this.error.set(getApiErrorMessage(error, 'No se pudieron cargar tus logros.'))
      });
  }
}
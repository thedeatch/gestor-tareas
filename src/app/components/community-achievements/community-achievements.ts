import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, DestroyRef, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { catchError, finalize, forkJoin, of } from 'rxjs';

import { getApiErrorMessage } from '../../models/api-error.model';

interface PersonalStats {
  posts: number;
  comments: number;
  points: number;
  reactionsReceived: number;
  eventRegistrations: number;
  achievements: number;
}

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
  imports: [DatePipe, DecimalPipe, MatButtonModule, MatCardModule],
  templateUrl: './community-achievements.html',
  styles: [`
    :host { display: block; margin-bottom: 1rem; }
    dl { display: grid; grid-template-columns: repeat(auto-fit, minmax(min(100%, 9rem), 1fr)); gap: .75rem; }
    dl div { border: 1px solid var(--mat-sys-outline-variant); border-radius: .75rem; padding: .75rem; }
    dd { margin: .25rem 0 0; font-size: 1.4rem; font-weight: 600; }
    li { margin-block: .75rem; }
    li p { margin-block: .25rem; }
    progress { max-width: 100%; margin-inline-end: .5rem; }
  `]
})
export class CommunityAchievements {
  readonly groupId = input.required<number>();
  private readonly http = inject(HttpClient);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly stats = signal<PersonalStats | null>(null);
  protected readonly cards: { key: keyof PersonalStats; label: string }[] = [
    { key: 'posts', label: 'Publicaciones acumuladas' },
    { key: 'comments', label: 'Comentarios acumulados' },
    { key: 'points', label: 'Puntos acumulados' },
    { key: 'reactionsReceived', label: 'Reacciones recibidas' },
    { key: 'eventRegistrations', label: 'Inscripciones a eventos' },
    { key: 'achievements', label: 'Logros obtenidos' }
  ];
  protected readonly items = signal<Achievement[] | null>(null);
  protected readonly loading = signal(false);
  protected readonly error = signal('');

  protected load(): void {
    if (this.loading()) return;
    this.loading.set(true);
    this.error.set('');
    const base = `/api/communities/${this.groupId()}`;
    forkJoin({
      items: this.http.get<Achievement[]>(`${base}/achievements/mine`),
      stats: this.http.get<PersonalStats>(`${base}/metrics/mine`).pipe(catchError(error => {
        this.error.set(getApiErrorMessage(error, 'No se pudieron cargar tus estadísticas.'));
        return of(null);
      }))
    }).pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.loading.set(false))).subscribe({
      next: ({ items, stats }) => {
        this.items.set(items);
        this.stats.set(stats);
      },
      error: error => this.error.set(getApiErrorMessage(error, 'No se pudo cargar tu actividad personal.'))
    });
  }
}
import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, DestroyRef, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
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
  imports: [DatePipe, DecimalPipe, MatButtonModule, MatCardModule, MatIconModule, MatProgressBarModule],
  templateUrl: './community-achievements.html',
  styleUrl: './community-achievements.css'
})
export class CommunityAchievements {
  readonly groupId = input.required<number>();
  private readonly http = inject(HttpClient);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly stats = signal<PersonalStats | null>(null);
  protected readonly cards: { key: keyof PersonalStats; label: string; icon: string }[] = [
    { key: 'posts', label: 'Publicaciones', icon: 'article' },
    { key: 'comments', label: 'Comentarios', icon: 'forum' },
    { key: 'points', label: 'Puntos', icon: 'stars' },
    { key: 'reactionsReceived', label: 'Reacciones', icon: 'favorite' },
    { key: 'eventRegistrations', label: 'Eventos', icon: 'event_available' },
    { key: 'achievements', label: 'Logros', icon: 'emoji_events' }
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

  protected progress(item: Achievement): number {
    if (item.target <= 0) return 100;
    return Math.min(100, Math.max(0, item.current / item.target * 100));
  }
}

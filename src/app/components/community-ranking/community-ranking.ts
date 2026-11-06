import { DatePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, DestroyRef, inject, input, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { finalize } from 'rxjs';

import { getApiErrorMessage } from '../../models/api-error.model';

type Badge = 'FIRST_POST' | 'CONVERSATION' | 'CONTRIBUTOR';

interface RankingEntry {
  userId: number;
  name: string;
  position: number | null;
  posts: number;
  comments: number;
  points: number;
  badges: Badge[];
  viewer: boolean;
}

interface Ranking {
  season: string;
  firstSeason: string;
  currentSeason: string;
  leaders: RankingEntry[];
  viewer: RankingEntry | null;
}

@Component({
  selector: 'app-community-ranking',
  imports: [DatePipe, MatButtonModule, MatCardModule],
  templateUrl: './community-ranking.html',
  styles: [`
    :host { display: block; margin-bottom: 1rem; }
    nav { display: flex; align-items: center; flex-wrap: wrap; gap: .5rem; }
    li { padding-block: .35rem; overflow-wrap: anywhere; }
    .badge { display: inline-block; padding: .15rem .5rem; margin: .2rem; border-radius: 1rem;
      background: var(--mat-sys-secondary-container); color: var(--mat-sys-on-secondary-container); }
  `]
})
export class CommunityRanking implements OnInit {
  readonly groupId = input.required<number>();
  private readonly http = inject(HttpClient);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly ranking = signal<Ranking | null>(null);
  protected readonly loading = signal(false);
  protected readonly error = signal('');
  protected readonly badgeLabels: Record<Badge, string> = {
    FIRST_POST: 'Primera publicación',
    CONVERSATION: 'Conversación activa',
    CONTRIBUTOR: 'Gran aporte'
  };

  ngOnInit(): void {
    this.load();
  }

  protected load(season?: string): void {
    if (this.loading()) return;
    this.loading.set(true);
    this.error.set('');
    this.http.get<Ranking>(`/api/communities/${this.groupId()}/ranking`, {
      params: season ? { season } : {}
    }).pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.loading.set(false))).subscribe({
      next: ranking => this.ranking.set(ranking),
      error: error => this.error.set(getApiErrorMessage(error, 'No se pudo cargar el ranking.'))
    });
  }

  protected move(season: string, delta: number): void {
    const [year, month] = season.split('-').map(Number);
    const adjacent = new Date(Date.UTC(year, month - 1 + delta, 1));
    this.load(adjacent.toISOString().slice(0, 7));
  }
}
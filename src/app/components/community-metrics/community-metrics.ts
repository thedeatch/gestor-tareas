import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, DestroyRef, inject, input, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { finalize } from 'rxjs';

import { getApiErrorMessage } from '../../models/api-error.model';

interface Counts {
  members: number;
  newMembers: number;
  posts: number;
  comments: number;
  events: number;
  registrations: number;
  participants: number;
}

interface Metrics {
  days: number;
  from: string;
  until: string;
  counts: Counts;
}

@Component({
  selector: 'app-community-metrics',
  imports: [DatePipe, DecimalPipe, MatButtonModule, MatCardModule],
  templateUrl: './community-metrics.html',
  styles: [`
    :host { display: block; margin-bottom: 1rem; }
    .filters { display: flex; flex-wrap: wrap; gap: .5rem; }
    .filters button[aria-pressed=true] {
      background: var(--mat-sys-secondary-container); color: var(--mat-sys-on-secondary-container);
    }
    dl { display: grid; grid-template-columns: repeat(auto-fit, minmax(min(100%, 9rem), 1fr)); gap: .75rem; }
    dl div { border: 1px solid var(--mat-sys-outline-variant); border-radius: .75rem; padding: .75rem; }
    dd { margin: .25rem 0 0; font-size: 1.5rem; font-weight: 600; }
  `]
})
export class CommunityMetrics implements OnInit {
  readonly groupId = input.required<number>();
  private readonly http = inject(HttpClient);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly metrics = signal<Metrics | null>(null);
  protected readonly selectedDays = signal(30);
  protected readonly loading = signal(false);
  protected readonly error = signal('');
  protected readonly periods = [7, 30, 90];
  protected readonly cards: { key: keyof Counts; label: string }[] = [
    { key: 'members', label: 'Miembros actuales' },
    { key: 'newMembers', label: 'Nuevas membresías' },
    { key: 'posts', label: 'Publicaciones' },
    { key: 'comments', label: 'Comentarios' },
    { key: 'events', label: 'Eventos iniciados' },
    { key: 'registrations', label: 'Inscripciones a eventos' },
    { key: 'participants', label: 'Personas inscritas' }
  ];

  ngOnInit(): void {
    this.load(30);
  }

  protected load(days: number): void {
    if (this.loading()) return;
    this.selectedDays.set(days);
    this.loading.set(true);
    this.error.set('');
    this.http.get<Metrics>(`/api/communities/${this.groupId()}/metrics`, { params: { days } })
      .pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.loading.set(false))).subscribe({
        next: metrics => this.metrics.set(metrics),
        error: error => this.error.set(getApiErrorMessage(error, 'No se pudieron cargar las estadísticas.'))
      });
  }
}
import { DatePipe, ViewportScroller } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, DestroyRef, inject, input, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { Subscription } from 'rxjs';

import { getApiErrorMessage } from '../../models/api-error.model';

interface Activity {
  type: 'POST' | 'EVENT' | 'ALBUM';
  resourceId: number;
  title: string;
  createdAt: string;
}

@Component({
  selector: 'app-community-activity',
  imports: [DatePipe, MatButtonModule, MatCardModule],
  templateUrl: './community-activity.html',
  styles: [`
    :host { display: block; margin-bottom: 1rem; }
    li { padding-block: .5rem; overflow-wrap: anywhere; }
    time { margin-inline-start: .5rem; }
  `]
})
export class CommunityActivity implements OnInit {
  readonly groupId = input.required<number>();
  private readonly http = inject(HttpClient);
  private readonly destroyRef = inject(DestroyRef);
  private readonly viewport = inject(ViewportScroller);
  private request?: Subscription;
  protected readonly items = signal<Activity[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal('');
  protected readonly labels = { POST: 'Publicación', EVENT: 'Evento', ALBUM: 'Álbum' };
  private readonly anchors = { POST: 'publicacion', EVENT: 'evento', ALBUM: 'album' };

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.request?.unsubscribe();
    this.loading.set(true);
    this.error.set('');
    this.request = this.http.get<Activity[]>(`/api/communities/${this.groupId()}/activity`)
      .pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: items => {
          this.items.set(items);
          this.loading.set(false);
        },
        error: error => {
          this.error.set(getApiErrorMessage(error, 'No se pudo cargar la actividad.'));
          this.loading.set(false);
        }
      });
  }

  protected goTo(item: Activity): void {
    this.viewport.scrollToAnchor(`${this.anchors[item.type]}-${item.resourceId}`);
  }
}
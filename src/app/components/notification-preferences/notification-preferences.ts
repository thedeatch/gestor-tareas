import { HttpClient } from '@angular/common/http';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { finalize } from 'rxjs';

import { getApiErrorMessage } from '../../models/api-error.model';
import { NotificationType } from '../../models/notification.model';

@Component({
  selector: 'app-notification-preferences',
  imports: [MatButtonModule, MatCardModule, MatSlideToggleModule],
  templateUrl: './notification-preferences.html',
  styles: [`
    :host { display: block; }
    .preferences { display: grid; gap: .75rem; margin-block: 1rem; }
  `]
})
export class NotificationPreferences {
  private readonly http = inject(HttpClient);
  private readonly destroyRef = inject(DestroyRef);
  private readonly url = '/api/users/me/notification-preferences';
  protected readonly disabled = signal<NotificationType[] | null>(null);
  protected readonly busy = signal(false);
  protected readonly error = signal('');
  protected readonly message = signal('');
  protected readonly options: { type: NotificationType; label: string }[] = [
    { type: 'INVITATION', label: 'Invitaciones y solicitudes de grupo' },
    { type: 'TASK', label: 'Tareas' },
    { type: 'COMMENT', label: 'Comentarios y respuestas' },
    { type: 'REACTION', label: 'Reacciones' },
    { type: 'EVENT', label: 'Eventos' }
  ];

  protected load(): void {
    if (this.busy()) return;
    this.busy.set(true);
    this.error.set('');
    this.http.get<NotificationType[]>(this.url)
      .pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.busy.set(false))).subscribe({
        next: types => this.disabled.set(types),
        error: error => this.error.set(getApiErrorMessage(error, 'No se pudieron cargar tus preferencias.'))
      });
  }

  protected update(type: NotificationType, enabled: boolean): void {
    const previous = this.disabled();
    if (!previous || this.busy()) return;

    this.disabled.set(enabled ? previous.filter(item => item !== type) : [...previous, type]);
    this.busy.set(true);
    this.error.set('');
    this.message.set('');
    this.http.put<void>(`${this.url}/${type}`, { enabled })
      .pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.busy.set(false))).subscribe({
        next: () => this.message.set('Preferencia guardada.'),
        error: error => {
          this.disabled.set(previous);
          this.error.set(getApiErrorMessage(error, 'No se pudo guardar la preferencia.'));
        }
      });
  }
}
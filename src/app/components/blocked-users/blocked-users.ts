import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { finalize } from 'rxjs';

import { BlockedUser, UserBlocksService } from '../../services/user-blocks';
import { getApiErrorMessage } from '../../models/api-error.model';

@Component({
  selector: 'app-blocked-users',
  imports: [MatButtonModule, MatCardModule],
  template: `
    <mat-card>
      <mat-card-header><mat-card-title>Usuarios bloqueados</mat-card-title></mat-card-header>
      <mat-card-content>
        <button mat-button type="button" (click)="load()" [disabled]="busy()">
          {{ users() === null ? 'Ver bloqueos' : 'Actualizar lista' }}
        </button>
        @if (error()) { <p role="alert">{{ error() }}</p> }
        @if (users(); as items) {
          <ul>
            @for (user of items; track user.id) {
              <li>
                {{ user.name }}
                <button mat-button type="button" [disabled]="busy()" (click)="unblock(user)">Desbloquear</button>
              </li>
            } @empty {
              <li>No tienes usuarios bloqueados.</li>
            }
          </ul>
        }
      </mat-card-content>
    </mat-card>
  `
})
export class BlockedUsers {
  private readonly blocks = inject(UserBlocksService);
  protected readonly users = signal<BlockedUser[] | null>(null);
  protected readonly busy = signal(false);
  protected readonly error = signal('');

  protected load(): void {
    this.busy.set(true);
    this.error.set('');
    this.blocks.list().pipe(finalize(() => this.busy.set(false))).subscribe({
      next: users => this.users.set(users),
      error: error => this.error.set(getApiErrorMessage(error, 'No se pudo cargar la lista.'))
    });
  }

  protected unblock(user: BlockedUser): void {
    this.busy.set(true);
    this.error.set('');
    this.blocks.set(user.id, false).pipe(finalize(() => this.busy.set(false))).subscribe({
      next: () => this.users.update(users => users?.filter(item => item.id !== user.id) ?? []),
      error: error => this.error.set(getApiErrorMessage(error, 'No se pudo desbloquear al usuario.'))
    });
  }
}
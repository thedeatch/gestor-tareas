import { HttpClient } from '@angular/common/http';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

interface ImageLiker {
  userId: number;
  name: string;
  hasAvatar: boolean;
}

export interface GalleryImageLikesDialogData {
  imageName: string;
  url: string;
}

@Component({
  selector: 'app-gallery-image-likes-dialog',
  imports: [RouterLink, MatDialogModule, MatButtonModule, MatIconModule],
  template: `
    <header mat-dialog-title>
      <div>
        <strong>Personas que indicaron Me gusta</strong>
        <span>{{ data.imageName }}</span>
      </div>
      <button mat-icon-button type="button" aria-label="Cerrar listado" (click)="dialogRef.close()">
        <mat-icon>close</mat-icon>
      </button>
    </header>
    <mat-dialog-content>
      @if (error()) {
        <div class="state" role="alert"><mat-icon>error_outline</mat-icon>{{ error() }}</div>
      } @else if (people(); as items) {
        <div class="people">
          @for (person of items; track person.userId) {
            <a [routerLink]="['/usuarios', person.userId]" (click)="dialogRef.close()">
              @if (avatarUrls()[person.userId]; as avatarUrl) {
                <img [src]="avatarUrl" alt="">
              } @else {
                <span class="avatar" aria-hidden="true">{{ person.name.charAt(0).toUpperCase() }}</span>
              }
              <span><strong>{{ person.name }}</strong><small>Ver perfil</small></span>
              <mat-icon>chevron_right</mat-icon>
            </a>
          } @empty {
            <div class="state"><mat-icon>favorite_border</mat-icon>Aún no hay reacciones.</div>
          }
        </div>
      } @else {
        <div class="state" role="status"><mat-icon>hourglass_top</mat-icon>Cargando personas…</div>
      }
    </mat-dialog-content>
  `,
  styles: [`
    header { display: flex; align-items: center; justify-content: space-between; gap: 1rem; }
    header > div { display: flex; min-width: 0; flex-direction: column; }
    header span { color: var(--mat-sys-on-surface-variant); font-size: .82rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    mat-dialog-content { min-width: min(360px, 80vw); padding: 0 .75rem 1rem !important; }
    .people { display: grid; gap: .25rem; }
    a { display: flex; align-items: center; gap: .75rem; padding: .65rem; border-radius: 12px; color: inherit; text-decoration: none; }
    a:hover { background: var(--mat-sys-surface-container-high); }
    a:focus-visible { outline: 2px solid var(--mat-sys-primary); }
    img, .avatar { width: 42px; height: 42px; flex: none; border-radius: 50%; }
    img { object-fit: cover; }
    .avatar { display: grid; place-items: center; background: var(--mat-sys-primary-container); color: var(--mat-sys-on-primary-container); font-weight: 700; }
    a > span:nth-child(2) { display: flex; min-width: 0; flex: 1; flex-direction: column; }
    a strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    a small { color: var(--mat-sys-on-surface-variant); }
    .state { display: flex; min-height: 120px; align-items: center; justify-content: center; gap: .5rem; color: var(--mat-sys-on-surface-variant); }
  `]
})
export class GalleryImageLikesDialog {
  readonly data = inject<GalleryImageLikesDialogData>(MAT_DIALOG_DATA);
  readonly dialogRef = inject(MatDialogRef<GalleryImageLikesDialog>);
  protected readonly people = signal<ImageLiker[] | null>(null);
  protected readonly avatarUrls = signal<Record<number, string>>({});
  protected readonly error = signal('');
  private readonly http = inject(HttpClient);
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    this.destroyRef.onDestroy(() => Object.values(this.avatarUrls()).forEach(url => URL.revokeObjectURL(url)));
    this.http.get<ImageLiker[]>(this.data.url).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: people => {
        this.people.set(people);
        people.filter(person => person.hasAvatar).forEach(person => this.loadAvatar(person.userId));
      },
      error: () => this.error.set('No fue posible cargar las personas que reaccionaron.')
    });
  }

  private loadAvatar(userId: number): void {
    this.http.get(`/api/users/${userId}/avatar`, { responseType: 'blob' })
      .pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: blob => this.avatarUrls.update(urls => ({ ...urls, [userId]: URL.createObjectURL(blob) })),
        error: () => {}
      });
  }
}

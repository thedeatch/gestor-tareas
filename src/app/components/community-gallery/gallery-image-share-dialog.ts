import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { getApiErrorMessage } from '../../models/api-error.model';

export interface GalleryImageShareDialogData {
  imageName: string;
  url: string;
}

@Component({
  selector: 'app-gallery-image-share-dialog',
  imports: [MatDialogModule, MatButtonModule, MatIconModule, MatInputModule],
  template: `
    <h2 mat-dialog-title>Compartir fotografía</h2>
    <mat-dialog-content>
      <p class="description">
        Crea un enlace para que cualquier persona pueda ver únicamente
        <strong>{{ data.imageName }}</strong>, incluso sin iniciar sesión.
      </p>

      @if (publicUrl()) {
        <label for="public-image-url">Enlace público</label>
        <div class="link-row">
          <input id="public-image-url" matInput readonly [value]="publicUrl()">
          <button mat-icon-button type="button" aria-label="Copiar enlace" title="Copiar enlace"
            (click)="copyLink()"><mat-icon>content_copy</mat-icon></button>
        </div>
        <p class="privacy"><mat-icon>visibility</mat-icon> El enlace no muestra el resto del álbum ni del grupo.</p>
      } @else {
        <p class="privacy"><mat-icon>link</mat-icon> El enlace será difícil de adivinar y podrás desactivarlo.</p>
      }

      @if (message()) { <p class="message" aria-live="polite">{{ message() }}</p> }
      @if (error()) { <p class="error" role="alert">{{ error() }}</p> }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close type="button">Cerrar</button>
      @if (publicUrl()) {
        <button mat-button type="button" class="revoke" [disabled]="loading()" (click)="revoke()">
          Desactivar enlace
        </button>
        <button mat-flat-button type="button" [disabled]="loading()" (click)="share()">
          <mat-icon>share</mat-icon> Compartir
        </button>
      } @else {
        <button mat-flat-button type="button" [disabled]="loading()" (click)="createLink()">
          <mat-icon>link</mat-icon> Crear enlace público
        </button>
      }
    </mat-dialog-actions>
  `,
  styles: [`
    mat-dialog-content { width: min(520px, 80vw); }
    .description { line-height: 1.5; }
    label { display: block; margin-block: 1rem .4rem; font-size: .78rem; font-weight: 700; }
    .link-row { display: flex; align-items: center; gap: .35rem; }
    input { width: 100%; min-width: 0; padding: .75rem; border: 1px solid var(--mat-sys-outline); border-radius: 8px; background: var(--mat-sys-surface-container); color: var(--mat-sys-on-surface); }
    .privacy { display: flex; align-items: center; gap: .45rem; color: var(--mat-sys-on-surface-variant); font-size: .82rem; }
    .privacy mat-icon { width: 18px; height: 18px; font-size: 18px; }
    .message { color: var(--mat-sys-primary); }
    .error, .revoke { color: var(--mat-sys-error); }
    mat-dialog-actions mat-icon { margin: 0; }
  `]
})
export class GalleryImageShareDialog {
  readonly data = inject<GalleryImageShareDialogData>(MAT_DIALOG_DATA);
  readonly publicUrl = signal('');
  readonly loading = signal(false);
  readonly message = signal('');
  readonly error = signal('');
  private readonly http = inject(HttpClient);

  createLink(): void {
    this.loading.set(true);
    this.error.set('');
    this.http.post<{ token: string }>(this.data.url, {}).subscribe({
      next: ({ token }) => {
        this.publicUrl.set(`${window.location.origin}/foto-compartida/${token}`);
        this.loading.set(false);
      },
      error: error => {
        this.error.set(getApiErrorMessage(error, 'No fue posible crear el enlace público.'));
        this.loading.set(false);
      }
    });
  }

  async copyLink(): Promise<void> {
    await navigator.clipboard.writeText(this.publicUrl());
    this.message.set('Enlace copiado.');
  }

  async share(): Promise<void> {
    const url = this.publicUrl();
    if (navigator.share) {
      try {
        await navigator.share({ title: this.data.imageName, text: 'Mira esta fotografía', url });
        return;
      } catch (error) {
        if (error instanceof DOMException && error.name === 'AbortError') return;
      }
    }
    await this.copyLink();
  }

  revoke(): void {
    this.loading.set(true);
    this.error.set('');
    this.http.delete<void>(this.data.url).subscribe({
      next: () => {
        this.publicUrl.set('');
        this.message.set('El enlace público fue desactivado.');
        this.loading.set(false);
      },
      error: error => {
        this.error.set(getApiErrorMessage(error, 'No fue posible desactivar el enlace.'));
        this.loading.set(false);
      }
    });
  }
}

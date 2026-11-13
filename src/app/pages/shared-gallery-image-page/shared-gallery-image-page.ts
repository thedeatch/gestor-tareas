import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

interface PublicGalleryImage {
  name: string;
  albumTitle: string;
  communityName: string;
}

@Component({
  selector: 'app-shared-gallery-image-page',
  imports: [RouterLink, MatButtonModule, MatIconModule],
  template: `
    <main>
      <header><a routerLink="/acerca-de" class="brand"><mat-icon>task_alt</mat-icon> GestorTareas</a></header>
      <section class="card">
        @if (loading()) {
          <div class="state"><mat-icon>hourglass_top</mat-icon><h1>Cargando fotografía…</h1></div>
        } @else if (image(); as photo) {
          <div class="metadata">
            <span>{{ photo.communityName }}</span>
            <h1>{{ photo.albumTitle }}</h1>
            <p>{{ photo.name }}</p>
          </div>
          <img [src]="contentUrl" [alt]="photo.name">
          <div class="actions">
            <button mat-flat-button type="button" (click)="share()"><mat-icon>share</mat-icon> Compartir</button>
          </div>
        } @else {
          <div class="state">
            <mat-icon>link_off</mat-icon>
            <h1>Este enlace ya no está disponible</h1>
            <p>Puede haber sido desactivado o la fotografía fue eliminada.</p>
          </div>
        }
      </section>
    </main>
  `,
  styles: [`
    :host { display: block; min-height: 100dvh; background: var(--mat-sys-surface); color: var(--mat-sys-on-surface); }
    main { width: min(1100px, calc(100% - 2rem)); margin: auto; padding-block: 1rem 2rem; }
    header { padding-block: .5rem 1.5rem; }
    .brand { display: inline-flex; align-items: center; gap: .45rem; color: inherit; font-size: 1.15rem; font-weight: 700; text-decoration: none; }
    .card { overflow: hidden; border: 1px solid var(--mat-sys-outline-variant); border-radius: 20px; background: var(--mat-sys-surface-container); box-shadow: var(--mat-sys-level2); }
    .metadata { padding: 1.2rem 1.4rem .8rem; text-align: center; }
    .metadata span { color: var(--mat-sys-primary); font-size: .75rem; font-weight: 700; letter-spacing: .08em; text-transform: uppercase; }
    h1, p { margin: .25rem 0; }
    img { display: block; width: 100%; max-height: 72dvh; object-fit: contain; background: #000; }
    .actions { display: flex; justify-content: center; padding: 1rem; }
    .actions mat-icon { margin: 0; }
    .state { display: grid; min-height: 55dvh; place-items: center; align-content: center; padding: 2rem; text-align: center; }
    .state mat-icon { width: 52px; height: 52px; font-size: 52px; color: var(--mat-sys-primary); }
  `]
})
export class SharedGalleryImagePage {
  readonly image = signal<PublicGalleryImage | null>(null);
  readonly loading = signal(true);
  readonly token = inject(ActivatedRoute).snapshot.paramMap.get('token') ?? '';
  readonly contentUrl = `/api/public/gallery-images/${this.token}/content`;
  private readonly http = inject(HttpClient);

  constructor() {
    this.http.get<PublicGalleryImage>(`/api/public/gallery-images/${this.token}`).subscribe({
      next: image => { this.image.set(image); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  async share(): Promise<void> {
    if (navigator.share) {
      try {
        await navigator.share({ title: this.image()?.name ?? 'Fotografía compartida', url: window.location.href });
        return;
      } catch (error) {
        if (error instanceof DOMException && error.name === 'AbortError') return;
      }
    }
    await navigator.clipboard.writeText(window.location.href);
  }
}

import { Component, computed, inject, output, signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { GalleryImageComments } from './gallery-image-comments';
import { NavigationEnd, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { filter } from 'rxjs';

export interface GalleryDialogImage { id: number; name: string; url: string; }
export interface GalleryImageDialogData {
  albumTitle: string;
  images: GalleryDialogImage[];
  initialIndex: number;
  imagesUrl: string;
}

@Component({
  selector: 'app-gallery-image-dialog',
  imports: [MatDialogModule, MatButtonModule, MatIconModule, GalleryImageComments],
  template: `
    <header mat-dialog-title>
      <div><strong>{{ data.albumTitle }}</strong><span>{{ current().name }}</span></div>
      <div class="actions">
        <button mat-button type="button" (click)="commentsVisible.update(toggle)"
          aria-controls="image-comments" [attr.aria-expanded]="commentsVisible()">
          <mat-icon>chat_bubble_outline</mat-icon>
          {{ commentsVisible() ? 'Ocultar comentarios' : 'Ver comentarios' }}
        </button>
        <button mat-icon-button type="button" aria-label="Cerrar visor" (click)="dialogRef.close()">
          <mat-icon>close</mat-icon>
        </button>
      </div>
    </header>
    <mat-dialog-content [class.image-only]="!commentsVisible()">
      <div class="viewer">
        <div class="photo">
          <button mat-icon-button type="button" aria-label="Imagen anterior"
            [disabled]="data.images.length < 2" (click)="previous()"><mat-icon>chevron_left</mat-icon></button>
          <button class="image-button" type="button" (click)="commentsVisible.update(toggle)"
            [attr.aria-label]="commentsVisible() ? 'Ampliar foto' : 'Ver foto con comentarios'"
            [attr.title]="commentsVisible() ? 'Ampliar foto' : 'Ver comentarios'"
            aria-controls="image-comments" [attr.aria-expanded]="commentsVisible()">
            <img [src]="current().url" [alt]="current().name">
            <span class="zoom-hint" aria-hidden="true">
              <mat-icon>{{ commentsVisible() ? 'zoom_in' : 'zoom_out' }}</mat-icon>
            </span>
          </button>
          <button mat-icon-button type="button" aria-label="Imagen siguiente"
            [disabled]="data.images.length < 2" (click)="next()"><mat-icon>chevron_right</mat-icon></button>
        </div>
        <p class="position" aria-live="polite">{{ index() + 1 }} de {{ data.images.length }}</p>
      </div>
      @for (image of [current()]; track image.id) {
        <app-gallery-image-comments id="image-comments" [hidden]="!commentsVisible()"
          [url]="data.imagesUrl + '/' + image.id + '/comments'"
          (commentCountChanged)="commentCountChanged.emit({ imageId: image.id, count: $event })" />
      }
    </mat-dialog-content>
  `,
  styles: [`
    header { display: flex; align-items: center; justify-content: space-between; gap: 1rem; }
    header > div:first-child { display: flex; min-width: 0; flex-direction: column; }
    .actions { display: flex; align-items: center; flex-shrink: 0; gap: .25rem; }
    header span {
      color: var(--mat-sys-on-surface-variant); font-size: .85rem;
      overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
    }
    mat-dialog-content {
      display: grid; grid-template-columns: minmax(0, 1fr) 320px;
      gap: 1rem; padding-inline: 1rem !important; max-height: 80dvh;
    }
    mat-dialog-content.image-only { grid-template-columns: minmax(0, 1fr); }
    .viewer { align-self: center; }
    .photo { position: relative; }
    .photo > button[mat-icon-button] {
      position: absolute; top: 50%; transform: translateY(-50%); z-index: 1;
      background: var(--mat-sys-surface-container-high); opacity: .9;
    }
    .photo > button:first-child { left: .5rem; }
    .photo > button:last-child { right: .5rem; }
    .image-button {
      position: relative; display: block; width: 100%; height: max(220px, calc(88dvh - 160px));
      border: 0; border-radius: 12px; padding: 0; background: transparent; cursor: zoom-in;
    }
    .image-only .image-button { cursor: zoom-out; }
    .image-button:focus-visible { outline: 2px solid var(--mat-sys-primary); outline-offset: -2px; }
    img { display: block; width: 100%; height: 100%; border-radius: 12px; object-fit: contain; }
    .zoom-hint {
      position: absolute; bottom: .5rem; right: .5rem; display: grid; place-items: center;
      border-radius: 50%; padding: .5rem; color: var(--mat-sys-on-surface);
      background: var(--mat-sys-surface-container-high); pointer-events: none;
    }
    .position { margin: .5rem 0; color: var(--mat-sys-on-surface-variant); text-align: center; }
    app-gallery-image-comments { border-inline-start: 1px solid var(--mat-sys-outline-variant); padding-inline-start: 1rem; }
    app-gallery-image-comments[hidden] { display: none; }
    @media (max-width: 850px) {
      header { flex-wrap: wrap; gap: .25rem; }
      .actions { margin-inline-start: auto; }
      mat-dialog-content { grid-template-columns: minmax(0, 1fr); }
      .image-button { height: 42dvh; min-height: 180px; }
      .image-only .image-button { height: max(220px, calc(82dvh - 160px)); }
      app-gallery-image-comments {
        border-inline-start: 0; padding-inline-start: 0;
        border-block-start: 1px solid var(--mat-sys-outline-variant); padding-block-start: 1rem;
      }
    }
  `]
})
export class GalleryImageDialog {
  readonly commentCountChanged = output<{ imageId: number; count: number }>();
  readonly data = inject<GalleryImageDialogData>(MAT_DIALOG_DATA);
  readonly dialogRef = inject(MatDialogRef<GalleryImageDialog>);
  readonly index = signal(this.data.initialIndex);
  readonly commentsVisible = signal(true);
  readonly toggle = (value: boolean): boolean => !value;
  readonly current = computed(() => this.data.images[this.index()]);
  constructor() {
    inject(Router).events.pipe(
      filter(event => event instanceof NavigationEnd),
      takeUntilDestroyed()
    ).subscribe(() => this.dialogRef.close());
  }
  previous(): void { this.index.update(value => (value - 1 + this.data.images.length) % this.data.images.length); }
  next(): void { this.index.update(value => (value + 1) % this.data.images.length); }
}

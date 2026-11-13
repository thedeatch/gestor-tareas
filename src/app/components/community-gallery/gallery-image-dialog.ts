import { Component, ElementRef, computed, inject, output, signal, viewChild } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { GalleryImageComments } from './gallery-image-comments';
import { NavigationEnd, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { filter, fromEvent } from 'rxjs';
import { GalleryImageLike, GalleryImageLikes, GalleryImageLikeChange } from './gallery-image-like';
import { GalleryImageLikesDialog } from './gallery-image-likes-dialog';
import { GalleryImageShareDialog } from './gallery-image-share-dialog';

export interface GalleryDialogImage extends GalleryImageLikes { id: number; name: string; url: string; }
export interface GalleryImageDialogData {
  albumTitle: string;
  images: GalleryDialogImage[];
  initialIndex: number;
  imagesUrl: string;
}

@Component({
  selector: 'app-gallery-image-dialog',
  imports: [MatDialogModule, MatButtonModule, MatIconModule, GalleryImageComments, GalleryImageLike],
  template: `
    <header mat-dialog-title>
      <div class="metadata">
        <span class="eyebrow">Álbum</span>
        <strong>{{ data.albumTitle }}</strong>
        <span class="filename"><mat-icon>image</mat-icon>{{ current().name }}</span>
      </div>
      <div class="actions">
        <button mat-icon-button type="button" aria-label="Compartir fotografía" title="Compartir fotografía"
          (click)="openShare()"><mat-icon>share</mat-icon></button>
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
        <div #photoStage class="photo-stage">
          <div class="photo">
          <button class="nav-button previous" mat-icon-button type="button" aria-label="Imagen anterior"
            [disabled]="data.images.length < 2" (click)="previous()"><mat-icon>chevron_left</mat-icon></button>
          <button class="image-button" type="button" (click)="commentsVisible.update(toggle)"
            [attr.aria-label]="commentsVisible() ? 'Ampliar foto' : 'Ver foto con comentarios'"
            [attr.title]="commentsVisible() ? 'Ampliar foto' : 'Ver comentarios'"
            aria-controls="image-comments" [attr.aria-expanded]="commentsVisible()">
            <img [src]="current().url" [alt]="current().name">
          </button>
          <button class="nav-button next" mat-icon-button type="button" aria-label="Imagen siguiente"
            [disabled]="data.images.length < 2" (click)="next()"><mat-icon>chevron_right</mat-icon></button>
          <button class="fullscreen-button" mat-icon-button type="button"
            [attr.aria-label]="fullscreen() ? 'Salir de pantalla completa' : 'Ver imagen en pantalla completa'"
            [attr.title]="fullscreen() ? 'Salir de pantalla completa' : 'Pantalla completa'"
            (click)="toggleFullscreen()">
            <mat-icon>{{ fullscreen() ? 'fullscreen_exit' : 'fullscreen' }}</mat-icon>
          </button>
          </div>
        </div>
        <div class="viewer-footer">
          <div class="reaction-actions">
            <app-gallery-image-like [imageId]="current().id" [url]="data.imagesUrl + '/' + current().id + '/like'"
              [likeCount]="current().likeCount" [liked]="current().liked" [showCount]="false"
              (likesChanged)="updateLikes($event)" />
            @if (current().likeCount > 0) {
              <button class="people-button" mat-button type="button" (click)="openLikes()">
                {{ current().likeCount }} {{ current().likeCount === 1 ? 'persona' : 'personas' }}
              </button>
            }
          </div>
          <p class="position" aria-live="polite">
            <mat-icon>photo_library</mat-icon>
            <strong>{{ index() + 1 }}</strong><span>de</span><strong>{{ data.images.length }}</strong>
          </p>
        </div>
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
    .metadata { display: grid; min-width: 0; grid-template-columns: auto minmax(0, 1fr); align-items: center; column-gap: .6rem; }
    .metadata .eyebrow { grid-column: 1 / -1; color: var(--mat-sys-primary); font-size: .68rem; font-weight: 700; letter-spacing: .08em; text-transform: uppercase; }
    .metadata strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .metadata .filename { display: inline-flex; min-width: 0; align-items: center; gap: .25rem; padding: .2rem .5rem; border-radius: 999px; background: var(--mat-sys-surface-container-high); font-size: .75rem; }
    .filename mat-icon { width: 15px; height: 15px; flex: none; font-size: 15px; }
    .actions { display: flex; align-items: center; flex-shrink: 0; gap: .25rem; }
    mat-dialog-content {
      display: grid; grid-template-columns: minmax(0, 1fr) 320px;
      gap: 1rem; padding-inline: 1rem !important; max-height: 80dvh;
    }
    mat-dialog-content.image-only { grid-template-columns: minmax(0, 1fr); }
    .viewer { align-self: center; }
    .photo-stage { border-radius: 16px; background: #000; }
    .photo { position: relative; overflow: hidden; border-radius: 16px; }
    .nav-button, .fullscreen-button {
      position: absolute; z-index: 2; display: grid; width: 44px; height: 44px;
      place-items: center; padding: 0; background: color-mix(in srgb, var(--mat-sys-surface-container-high) 92%, transparent);
      box-shadow: 0 3px 12px #0005;
    }
    .nav-button { top: 50%; transform: translateY(-50%); }
    .nav-button.previous { left: 1rem; }
    .nav-button.next { right: 1rem; }
    .fullscreen-button { right: 1rem; bottom: 1rem; }
    .nav-button mat-icon, .fullscreen-button mat-icon { margin: 0; }
    .image-button {
      position: relative; display: block; width: 100%; height: max(220px, calc(88dvh - 160px));
      border: 0; border-radius: 12px; padding: 0; background: transparent; cursor: zoom-in;
    }
    .image-only .image-button { cursor: zoom-out; }
    .image-button:focus-visible { outline: 2px solid var(--mat-sys-primary); outline-offset: -2px; }
    img { display: block; width: 100%; height: 100%; border-radius: 12px; object-fit: contain; }
    .viewer-footer { position: relative; display: flex; min-height: 50px; align-items: center; justify-content: center; padding: .35rem 0; }
    .reaction-actions { display: flex; align-items: center; gap: .15rem; }
    .people-button { color: var(--mat-sys-on-surface-variant); font-size: .82rem; }
    .position { position: absolute; right: 0; display: flex; align-items: center; gap: .3rem; margin: 0; padding: .32rem .6rem; border-radius: 999px; background: var(--mat-sys-surface-container-high); color: var(--mat-sys-on-surface-variant); font-size: .8rem; }
    .position mat-icon { width: 16px; height: 16px; font-size: 16px; }
    .position strong { color: var(--mat-sys-on-surface); }
    .photo-stage:fullscreen { display: grid; width: 100vw; height: 100vh; place-items: center; border-radius: 0; }
    .photo-stage:fullscreen .photo { width: 100%; height: 100%; border-radius: 0; }
    .photo-stage:fullscreen .image-button { height: 100vh; }
    .photo-stage:fullscreen img { border-radius: 0; }
    app-gallery-image-comments { border-inline-start: 1px solid var(--mat-sys-outline-variant); padding-inline-start: 1rem; }
    app-gallery-image-comments[hidden] { display: none; }
    @media (max-width: 850px) {
      header { flex-wrap: wrap; gap: .25rem; }
      .actions { margin-inline-start: auto; }
      mat-dialog-content { grid-template-columns: minmax(0, 1fr); }
      .image-button { height: 42dvh; min-height: 180px; }
      .image-only .image-button { height: max(220px, calc(82dvh - 160px)); }
      .viewer-footer { justify-content: flex-start; }
      .position { right: 0; }
      app-gallery-image-comments {
        border-inline-start: 0; padding-inline-start: 0;
        border-block-start: 1px solid var(--mat-sys-outline-variant); padding-block-start: 1rem;
      }
    }
  `]
})
export class GalleryImageDialog {
  readonly commentCountChanged = output<{ imageId: number; count: number }>();
  readonly likesChanged = output<GalleryImageLikeChange>();
  readonly data = inject<GalleryImageDialogData>(MAT_DIALOG_DATA);
  readonly dialogRef = inject(MatDialogRef<GalleryImageDialog>);
  readonly index = signal(this.data.initialIndex);
  readonly commentsVisible = signal(true);
  readonly toggle = (value: boolean): boolean => !value;
  readonly images = signal(this.data.images);
  readonly current = computed(() => this.images()[this.index()]);
  readonly fullscreen = signal(false);
  private readonly photoStage = viewChild<ElementRef<HTMLElement>>('photoStage');
  private readonly dialog = inject(MatDialog);
  constructor() {
    inject(Router).events.pipe(
      filter(event => event instanceof NavigationEnd),
      takeUntilDestroyed()
    ).subscribe(() => this.dialogRef.close());
    fromEvent(document, 'fullscreenchange').pipe(takeUntilDestroyed()).subscribe(() =>
      this.fullscreen.set(document.fullscreenElement === this.photoStage()?.nativeElement));
  }
  protected updateLikes(change: GalleryImageLikeChange): void {
    const { imageId, likes } = change;
    this.images.update(images => images.map(image => image.id === imageId ? { ...image, ...likes } : image));
    this.likesChanged.emit(change);
  }
  protected openLikes(): void {
    this.dialog.open(GalleryImageLikesDialog, {
      data: {
        imageName: this.current().name,
        url: `${this.data.imagesUrl}/${this.current().id}/likes`
      },
      width: 'min(440px, 94vw)', maxWidth: '94vw'
    });
  }
  protected openShare(): void {
    this.dialog.open(GalleryImageShareDialog, {
      data: {
        imageName: this.current().name,
        url: `${this.data.imagesUrl}/${this.current().id}/share`
      },
      width: 'min(560px, 94vw)', maxWidth: '94vw'
    });
  }
  protected async toggleFullscreen(): Promise<void> {
    if (document.fullscreenElement) {
      await document.exitFullscreen();
      this.fullscreen.set(false);
      return;
    }
    await this.photoStage()?.nativeElement.requestFullscreen();
  }
  previous(): void { this.index.update(value => (value - 1 + this.data.images.length) % this.data.images.length); }
  next(): void { this.index.update(value => (value + 1) % this.data.images.length); }
}

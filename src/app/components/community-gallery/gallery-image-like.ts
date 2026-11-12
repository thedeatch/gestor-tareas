import { HttpClient } from '@angular/common/http';
import { Component, DestroyRef, inject, input, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { finalize } from 'rxjs';
import { getApiErrorMessage } from '../../models/api-error.model';

export interface GalleryImageLikes {
  likeCount: number;
  liked: boolean;
}
export interface GalleryImageLikeChange {
  imageId: number;
  likes: GalleryImageLikes;
}

@Component({
  selector: 'app-gallery-image-like',
  imports: [MatButtonModule, MatIconModule, MatSnackBarModule],
  template: `
    <button mat-button type="button" [class.compact]="compact()" [disabled]="busy()"
      [attr.aria-pressed]="liked()"
      [attr.aria-label]="(liked() ? 'Quitar Me gusta de la foto, ' : 'Me gusta la foto, ') + likeCount() + ' Me gusta'"
      (click)="toggle()">
      <mat-icon>{{ liked() ? 'favorite' : 'favorite_border' }}</mat-icon>
      @if (!compact()) { Me gusta }
      {{ likeCount() }}
    </button>
  `,
  styles: [`
    :host { display: inline-block; }
    button[aria-pressed="true"] { color: var(--mat-sys-primary); }
    button.compact {
      min-width: 0; height: 30px; padding-inline: .5rem;
      border-radius: 999px; background: #000c; color: #fff; font-size: .78rem;
    }
    button.compact mat-icon { width: 16px; height: 16px; font-size: 16px; margin-right: .25rem; }
    button.compact[aria-pressed="true"] { color: #ffb3c1; }
    button.compact:disabled { opacity: .7; }
  `]
})
export class GalleryImageLike {
  readonly imageId = input.required<number>();
  readonly url = input.required<string>();
  readonly likeCount = input(0);
  readonly liked = input(false);
  readonly compact = input(false);
  readonly likesChanged = output<GalleryImageLikeChange>();
  protected readonly busy = signal(false);
  private readonly http = inject(HttpClient);
  private readonly destroyRef = inject(DestroyRef);
  private readonly snackBar = inject(MatSnackBar);

  protected toggle(): void {
    if (this.busy()) return;
    const imageId = this.imageId();
    this.busy.set(true);
    const request = this.liked()
      ? this.http.delete<GalleryImageLikes>(this.url())
      : this.http.put<GalleryImageLikes>(this.url(), null);
    request.pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.busy.set(false))).subscribe({
      next: likes => this.likesChanged.emit({ imageId, likes }),
      error: error => this.snackBar.open(
        getApiErrorMessage(error, 'No se pudo actualizar el Me gusta de la foto.'),
        'Cerrar', { duration: 5000 })
    });
  }
}

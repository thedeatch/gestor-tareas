import { DatePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { Subscription, finalize, of, switchMap } from 'rxjs';
import { SaveItemButton } from '../../components/save-item-button/save-item-button';
import { GalleryDialogImage, GalleryImageDialog } from '../../components/community-gallery/gallery-image-dialog';
import { getApiErrorMessage } from '../../models/api-error.model';
import { SavedItem, SavedItemsFilter, SavedItemsPage } from '../../models/saved-item.model';
import { SavedItemsService } from '../../services/saved-items';
import { LazyImage } from '../../components/lazy-image/lazy-image';

@Component({
  selector: 'app-saved-items-page',
  imports: [DatePipe, RouterLink, MatButtonModule, MatCardModule, MatIconModule, SaveItemButton, LazyImage],
  templateUrl: './saved-items-page.html',
  styleUrl: './saved-items-page.css'
})
export class SavedItemsPageComponent implements OnInit {
  private readonly saved = inject(SavedItemsService);
  private readonly http = inject(HttpClient);
  private readonly dialog = inject(MatDialog);
  private readonly destroyRef = inject(DestroyRef);
  private request?: Subscription;
  protected readonly kind = signal<SavedItemsFilter>('ALL');
  protected readonly filters: { kind: SavedItemsFilter; label: string }[] = [
    { kind: 'ALL', label: 'Todos' }, { kind: 'POST', label: 'Publicaciones' }, { kind: 'IMAGE', label: 'Fotos' },
    { kind: 'EVENT', label: 'Eventos' }
  ];
  protected readonly result = signal<SavedItemsPage | null>(null);
  protected readonly busy = signal(false);
  protected readonly opening = signal<number | null>(null);
  protected readonly error = signal('');
  ngOnInit(): void {
    this.load();
  }

  protected filter(kind: SavedItemsFilter): void {
    this.kind.set(kind);
    this.load();
  }

  protected load(page = 0): void {
    this.request?.unsubscribe();
    this.busy.set(true);
    this.error.set('');
    this.request = this.saved.findAll(this.kind(), page).pipe(
      switchMap(result => result.items.length === 0 && result.total > 0 && page > 0
        ? this.saved.findAll(this.kind(), Math.ceil(result.total / result.size) - 1) : of(result)),
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.busy.set(false))
    ).subscribe({
      next: result => {
        this.result.set(result);
      },
      error: error => this.error.set(getApiErrorMessage(error, 'No se pudieron cargar tus guardados.'))
    });
  }

  protected savedChanged(saved: boolean): void {
    if (!saved) this.load(this.result()?.page ?? 0);
  }

  protected openPhoto(item: SavedItem): void {
    if (this.opening() !== null) return;
    this.opening.set(item.targetId);
    this.error.set('');
    const imagesUrl = this.imagesUrl(item);
    this.http.get<GalleryDialogImage>(imagesUrl + '/' + item.targetId + '/details').pipe(
      takeUntilDestroyed(this.destroyRef), finalize(() => this.opening.set(null))
    ).subscribe({
      next: image => {
        this.dialog.open(GalleryImageDialog, {
          data: {
            groupId: item.groupId, albumTitle: item.excerpt, imagesUrl,
            images: [image], initialIndex: 0
          },
          width: 'min(1440px, 98vw)', maxWidth: '98vw', maxHeight: '96dvh'
        }).afterClosed().pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.load(this.result()?.page ?? 0));
      },
      error: error => this.error.set(getApiErrorMessage(error, 'No se pudo abrir la foto.'))
    });
  }

  private imagesUrl(item: SavedItem): string {
    return `/api/communities/${item.groupId}/albums/${item.albumId}/images`;
  }

  protected thumbnailUrl(item: SavedItem): string {
    return this.imagesUrl(item) + '/' + item.targetId + '/thumbnail';
  }
}

import { DatePipe } from '@angular/common';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Component, DestroyRef, OnInit, inject, input, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { Subscription, catchError, finalize, forkJoin, map, of, switchMap } from 'rxjs';
import { GalleryDialogImage, GalleryImageDialog } from '../community-gallery/gallery-image-dialog';
import { SaveItemButton } from '../save-item-button/save-item-button';
import { CommunityMemoriesPage, CommunityMemory } from '../../models/community-memory.model';
import { getApiErrorMessage } from '../../models/api-error.model';

@Component({
  selector: 'app-community-memories',
  imports: [DatePipe, MatButtonModule, MatCardModule, MatIconModule, SaveItemButton],
  templateUrl: './community-memories.html',
  styleUrl: './community-memories.css'
})
export class CommunityMemories implements OnInit {
  readonly groupId = input.required<number>();
  readonly openGallery = output<void>();
  private readonly http = inject(HttpClient);
  private readonly dialog = inject(MatDialog);
  private readonly destroyRef = inject(DestroyRef);
  private request?: Subscription;
  private previewRequest?: Subscription;
  protected readonly result = signal<CommunityMemoriesPage | null>(null);
  protected readonly anniversariesOnly = signal(false);
  protected readonly busy = signal(false);
  protected readonly opening = signal<number | null>(null);
  protected readonly error = signal('');
  protected readonly previews = signal<Record<number, string>>({});

  constructor() {
    this.destroyRef.onDestroy(() => this.revokePreviews());
  }

  ngOnInit(): void {
    this.load();
  }

  protected filter(anniversariesOnly: boolean): void {
    this.anniversariesOnly.set(anniversariesOnly);
    this.load();
  }

  protected load(page = 0): void {
    this.request?.unsubscribe();
    this.previewRequest?.unsubscribe();
    this.busy.set(true);
    this.error.set('');
    const request = (page: number) => this.http.get<CommunityMemoriesPage>(
      `/api/communities/${this.groupId()}/memories`, {
        params: new HttpParams().set('anniversariesOnly', this.anniversariesOnly()).set('page', page)
      });
    this.request = request(page).pipe(
      switchMap(result => !result.items.length && result.total > 0 && page > 0
        ? request(Math.ceil(result.total / result.size) - 1) : of(result)),
      takeUntilDestroyed(this.destroyRef), finalize(() => this.busy.set(false))
    ).subscribe({
      next: result => { this.result.set(result); this.loadPreviews(result.items); },
      error: error => this.error.set(getApiErrorMessage(error, 'No se pudieron cargar los recuerdos.'))
    });
  }

  protected view(memory: CommunityMemory): void {
    if (this.opening() !== null) return;
    this.opening.set(memory.imageId);
    this.error.set('');
    const imagesUrl = this.imagesUrl(memory);
    const preview = this.previews()[memory.imageId];
    const source = preview ? of(preview) : this.http.get(`${imagesUrl}/${memory.imageId}`,
      { responseType: 'blob' }).pipe(map(blob => {
        const existing = this.previews()[memory.imageId];
        if (existing) return existing;
        const url = URL.createObjectURL(blob);
        this.previews.update(urls => ({ ...urls, [memory.imageId]: url }));
        return url;
      }));
    forkJoin({ images: this.http.get<GalleryDialogImage[]>(imagesUrl), source }).pipe(
      takeUntilDestroyed(this.destroyRef), finalize(() => this.opening.set(null))
    ).subscribe({
      next: ({ images, source }) => {
        const image = images.find(image => image.id === memory.imageId);
        if (!image) {
          this.error.set('La foto ya no está disponible.');
          return;
        }
        this.dialog.open(GalleryImageDialog, {
          data: {
            groupId: this.groupId(), albumTitle: memory.albumTitle, imagesUrl,
            images: [{ ...image, url: source }], initialIndex: 0
          },
          width: 'min(1440px, 98vw)', maxWidth: '98vw', maxHeight: '96dvh'
        }).afterClosed().pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.load(this.result()?.page ?? 0));
      },
      error: error => this.error.set(getApiErrorMessage(error, 'No se pudo abrir la foto del recuerdo.'))
    });
  }

  private imagesUrl(memory: CommunityMemory): string {
    return `/api/communities/${this.groupId()}/albums/${memory.albumId}/images`;
  }

  private loadPreviews(items: CommunityMemory[]): void {
    this.revokePreviews();
    if (!items.length) return;
    this.previewRequest = forkJoin(items.map(memory =>
      this.http.get(`${this.imagesUrl(memory)}/${memory.imageId}`, { responseType: 'blob' }).pipe(
        map(blob => ({ id: memory.imageId, blob })), catchError(() => of(null))
      ))).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(images => {
        const urls = { ...this.previews() };
        for (const image of images) {
          if (image && !urls[image.id]) urls[image.id] = URL.createObjectURL(image.blob);
        }
        this.previews.set(urls);
      });
  }

  private revokePreviews(): void {
    for (const url of Object.values(this.previews())) URL.revokeObjectURL(url);
    this.previews.set({});
  }
}

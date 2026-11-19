import { DatePipe } from '@angular/common';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Component, DestroyRef, OnInit, inject, input, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { Subscription, finalize, of, switchMap } from 'rxjs';
import { GalleryDialogImage, GalleryImageDialog } from '../community-gallery/gallery-image-dialog';
import { SaveItemButton } from '../save-item-button/save-item-button';
import { CommunityMemoriesPage, CommunityMemory } from '../../models/community-memory.model';
import { getApiErrorMessage } from '../../models/api-error.model';
import { LazyImage } from '../lazy-image/lazy-image';

@Component({
  selector: 'app-community-memories',
  imports: [DatePipe, MatButtonModule, MatCardModule, MatIconModule, SaveItemButton, LazyImage],
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
  protected readonly result = signal<CommunityMemoriesPage | null>(null);
  protected readonly anniversariesOnly = signal(false);
  protected readonly busy = signal(false);
  protected readonly opening = signal<number | null>(null);
  protected readonly error = signal('');
  ngOnInit(): void {
    this.load();
  }

  protected filter(anniversariesOnly: boolean): void {
    this.anniversariesOnly.set(anniversariesOnly);
    this.load();
  }

  protected load(page = 0): void {
    this.request?.unsubscribe();
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
      next: result => { this.result.set(result); },
      error: error => this.error.set(getApiErrorMessage(error, 'No se pudieron cargar los recuerdos.'))
    });
  }

  protected view(memory: CommunityMemory): void {
    if (this.opening() !== null) return;
    this.opening.set(memory.imageId);
    this.error.set('');
    const imagesUrl = this.imagesUrl(memory);
    this.http.get<GalleryDialogImage>(imagesUrl + '/' + memory.imageId + '/details').pipe(
      takeUntilDestroyed(this.destroyRef), finalize(() => this.opening.set(null))
    ).subscribe({
      next: image => {
        this.dialog.open(GalleryImageDialog, {
          data: {
            groupId: this.groupId(), albumTitle: memory.albumTitle, imagesUrl,
            images: [image], initialIndex: 0
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

  protected thumbnailUrl(memory: CommunityMemory): string {
    return this.imagesUrl(memory) + '/' + memory.imageId + '/thumbnail';
  }
}

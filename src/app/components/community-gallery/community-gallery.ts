import { HttpClient } from '@angular/common/http';
import { Component, DestroyRef, inject, input, OnInit, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog } from '@angular/material/dialog';
import { catchError, concatMap, finalize, forkJoin, from, Observable, of, switchMap, toArray } from 'rxjs';

import { getApiErrorMessage } from '../../models/api-error.model';
import { GalleryImageDialog, GalleryDialogImage } from './gallery-image-dialog';
import { GalleryImageLike, GalleryImageLikes } from './gallery-image-like';

interface Album {
  id: number;
  title: string;
  description: string;
  imageCount: number;
  coverImageId: number | null;
}

interface AlbumImage extends GalleryImageLikes {
  id: number;
  name: string;
  commentCount: number;
}

@Component({
  selector: 'app-community-gallery',
  imports: [ReactiveFormsModule, MatButtonModule, MatCardModule, MatFormFieldModule,
    MatInputModule, MatIconModule, MatProgressSpinnerModule, GalleryImageLike],
  templateUrl: './community-gallery.html',
  styleUrl: './community-gallery.css'
})
export class CommunityGallery implements OnInit {
  readonly groupId = input.required<number>();
  readonly canManage = input(false);
  readonly albumCreated = output<void>();
  private readonly http = inject(HttpClient);
  private readonly destroyRef = inject(DestroyRef);
  private readonly dialog = inject(MatDialog);
  protected readonly albums = signal<Album[] | null>(null);
  protected readonly album = signal<Album | null>(null);
  protected readonly images = signal<AlbumImage[]>([]);
  protected readonly coverUrls = signal<Record<number, string>>({});
  protected readonly imageUrls = signal<Record<number, string>>({});
  protected readonly imagesLoading = signal(false);
  protected readonly busy = signal(false);
  protected readonly draggingImages = signal(false);
  protected readonly creating = signal(false);
  protected readonly error = signal('');
  protected readonly message = signal('');
  protected readonly form = new FormGroup({
    title: new FormControl('', {
      nonNullable: true, validators: [Validators.required, Validators.maxLength(120)]
    }),
    description: new FormControl('', {
      nonNullable: true, validators: [Validators.maxLength(500)]
    })
  });

  constructor() {
    this.destroyRef.onDestroy(() => {
      this.revokeUrls(this.coverUrls());
      this.revokeUrls(this.imageUrls());
    });
  }

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.run(this.http.get<Album[]>(this.url()), 'No se pudieron cargar los álbumes.',
      albums => this.setAlbums(albums));
  }

  protected create(): void {
    if (this.form.invalid || !this.form.controls.title.value.trim()) return;
    this.run(this.http.post<void>(this.url(), this.form.getRawValue()).pipe(switchMap(() => {
      this.form.reset();
      this.creating.set(false);
      this.message.set('Álbum creado.');
      this.albumCreated.emit();
      return this.http.get<Album[]>(this.url());
    })), 'No se pudo completar la actualización de los álbumes.', albums => this.setAlbums(albums));
  }

  protected open(album: Album): void {
    this.run(this.http.get<AlbumImage[]>(this.imagesUrl(album.id)), 'No se pudo abrir el álbum.', images => {
      this.album.set(album);
      this.images.set(images);
      this.loadImageUrls(album, images);
    });
  }

  protected upload(input: HTMLInputElement, album: Album): void {
    const files = Array.from(input.files ?? []);
    input.value = '';
    this.uploadFiles(files, album);
  }

  protected dragOver(event: DragEvent): void {
    event.preventDefault();
    if (!this.busy()) this.draggingImages.set(true);
  }

  protected dragLeave(event: DragEvent): void {
    event.preventDefault();
    this.draggingImages.set(false);
  }

  protected dropImages(event: DragEvent, album: Album): void {
    event.preventDefault();
    event.stopPropagation();
    this.draggingImages.set(false);
    this.uploadFiles(Array.from(event.dataTransfer?.files ?? []), album);
  }

  private uploadFiles(files: File[], album: Album): void {
    if (!files.length || this.busy()) return;
    const invalid = files.find(file => !['image/jpeg', 'image/png'].includes(file.type)
      || file.size > 2 * 1024 * 1024 || file.size === 0);
    if (invalid) {
      this.error.set('Todas las imágenes deben ser JPEG o PNG y pesar hasta 2 MB cada una.');
      return;
    }
    const uploads = files.map(file => {
      const body = new FormData();
      body.append('file', file);
      return body;
    });
    this.run(from(uploads).pipe(
      concatMap(body => this.http.post<AlbumImage>(this.imagesUrl(album.id), body)),
      toArray(),
      switchMap(() => this.http.get<AlbumImage[]>(this.imagesUrl(album.id)))),
    files.length === 1 ? 'No se pudo subir la imagen.' : 'No se pudieron subir las imágenes.', images => {
      this.images.set(images);
      this.loadImageUrls(album, images);
      this.albums.update(items => items?.map(item => item.id === album.id
        ? { ...item, imageCount: images.length, coverImageId: images[0]?.id ?? null } : item) ?? null);
      if (this.albums()) this.loadCovers(this.albums()!);
      this.message.set(files.length === 1
        ? 'Imagen añadida al álbum.'
        : `${files.length} imágenes añadidas al álbum.`);
    });
  }

  protected closeAlbum(): void {
    this.album.set(null);
    this.images.set([]);
    this.revokeUrls(this.imageUrls());
    this.imageUrls.set({});
  }

  protected view(image: AlbumImage): void {
    const selected = this.album();
    const available: GalleryDialogImage[] = this.images().filter(item => this.imageUrls()[item.id])
      .map(item => ({ ...item, url: this.imageUrls()[item.id] }));
    const index = available.findIndex(item => item.id === image.id);
    if (!selected || index < 0) return;
    const dialogRef = this.dialog.open(GalleryImageDialog, {
      data: {
        albumTitle: selected.title, images: available, initialIndex: index,
        imagesUrl: this.imagesUrl(selected.id)
      },
      width: 'min(1440px, 98vw)', maxWidth: '98vw', maxHeight: '96dvh'
    });
    dialogRef.componentInstance.commentCountChanged.subscribe(({ imageId, count }) => {
      if (this.album()?.id !== selected.id) return;
      this.images.update(images => images.map(item =>
        item.id === imageId ? { ...item, commentCount: count } : item));
    });
    dialogRef.componentInstance.likesChanged.subscribe(({ imageId, likes }) => {
      if (this.album()?.id === selected.id) this.updateLikes(imageId, likes);
    });
  }

  protected imageLikeUrl(imageId: number): string {
    return `${this.imagesUrl(this.album()!.id)}/${imageId}/like`;
  }

  protected updateLikes(imageId: number, likes: GalleryImageLikes): void {
    this.images.update(images => images.map(image =>
      image.id === imageId ? { ...image, ...likes } : image));
  }

  private setAlbums(albums: Album[]): void { this.albums.set(albums); this.loadCovers(albums); }

  private loadCovers(albums: Album[]): void {
    const requests = albums.filter(item => item.coverImageId).map(item =>
      this.http.get(`${this.imagesUrl(item.id)}/${item.coverImageId}`, { responseType: 'blob' })
        .pipe(catchError(() => of(null)), switchMap(blob => of({ id: item.id, blob }))));
    this.revokeUrls(this.coverUrls());
    this.coverUrls.set({});
    if (!requests.length) return;
    forkJoin(requests).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(items => {
      const urls: Record<number, string> = {};
      for (const item of items) if (item.blob) urls[item.id] = URL.createObjectURL(item.blob);
      this.coverUrls.set(urls);
    });
  }

  private loadImageUrls(album: Album, images: AlbumImage[]): void {
    this.revokeUrls(this.imageUrls());
    this.imageUrls.set({});
    if (!images.length) return;
    this.imagesLoading.set(true);
    forkJoin(images.map(image => this.http.get(`${this.imagesUrl(album.id)}/${image.id}`,
      { responseType: 'blob' }).pipe(catchError(() => of(null)),
      switchMap(blob => of({ id: image.id, blob }))))).pipe(
      takeUntilDestroyed(this.destroyRef), finalize(() => this.imagesLoading.set(false))
    ).subscribe(items => {
      const urls: Record<number, string> = {};
      for (const item of items) if (item.blob) urls[item.id] = URL.createObjectURL(item.blob);
      this.imageUrls.set(urls);
    });
  }

  private revokeUrls(urls: Record<number, string>): void {
    Object.values(urls).forEach(url => URL.revokeObjectURL(url));
  }

  private url(): string {
    return `/api/communities/${this.groupId()}/albums`;
  }

  private imagesUrl(albumId: number): string {
    return `${this.url()}/${albumId}/images`;
  }

  private run<T>(request: Observable<T>, fallback: string, success: (value: T) => void): void {
    if (this.busy()) return;
    this.error.set('');
    this.message.set('');
    this.busy.set(true);
    request.pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.busy.set(false))).subscribe({
      next: success,
      error: error => this.error.set(getApiErrorMessage(error, fallback))
    });
  }
}

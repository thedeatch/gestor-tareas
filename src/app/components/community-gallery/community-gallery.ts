import { HttpClient } from '@angular/common/http';
import { Component, DestroyRef, computed, inject, input, OnInit, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, Router } from '@angular/router';
import { concatMap, finalize, from, map, Observable, of, switchMap, tap, toArray } from 'rxjs';

import { getApiErrorMessage } from '../../models/api-error.model';
import { GalleryImageDialog, GalleryDialogImage } from './gallery-image-dialog';
import { GalleryImageLike, GalleryImageLikes } from './gallery-image-like';
import { SaveItemButton } from '../save-item-button/save-item-button';
import { PhotoDateFields } from '../photo-date-fields/photo-date-fields';
import { PhotoDateInfo, createPhotoDateForm, photoDateInput } from '../../models/photo-date.model';
import { SearchTarget } from '../../directives/search-target';
import { LazyImage } from '../lazy-image/lazy-image';

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
  date?: PhotoDateInfo;
}

@Component({
  selector: 'app-community-gallery',
  imports: [ReactiveFormsModule, MatButtonModule, MatCardModule, MatFormFieldModule,
    MatInputModule, MatIconModule, MatProgressSpinnerModule, GalleryImageLike, SaveItemButton, PhotoDateFields, SearchTarget, LazyImage],
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
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  protected readonly albums = signal<Album[] | null>(null);
  protected readonly album = signal<Album | null>(null);
  protected readonly images = signal<AlbumImage[]>([]);
  protected readonly imageLimit = signal(24);
  protected readonly albumLimit = signal(12);
  protected readonly visibleImages = computed(() => this.images().slice(0, this.imageLimit()));
  protected readonly visibleAlbums = computed(() => (this.albums() ?? []).slice(0, this.albumLimit()));
  protected readonly addImagePage = (value: number) => value + 24;
  protected readonly addAlbumPage = (value: number) => value + 12;
  protected readonly busy = signal(false);
  protected readonly draggingImages = signal(false);
  protected readonly creating = signal(false);
  protected readonly error = signal('');
  protected readonly message = signal('');
  protected readonly photoDateForm = createPhotoDateForm();
  protected readonly form = new FormGroup({
    title: new FormControl('', {
      nonNullable: true, validators: [Validators.required, Validators.maxLength(120)]
    }),
    description: new FormControl('', {
      nonNullable: true, validators: [Validators.maxLength(500)]
    })
  });

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.run(this.http.get<Album[]>(this.url()).pipe(
      tap(albums => this.setAlbums(albums)),
      switchMap(albums => {
        const requested = Number(this.route.snapshot.queryParamMap.get('album'));
        const album = albums.find(album => album.id === requested);
        if (!album) {
          if (requested > 0) this.error.set('El álbum seleccionado ya no está disponible.');
          return of(null);
        }
        return this.http.get<AlbumImage[]>(this.imagesUrl(album.id)).pipe(map(images => ({ album, images })));
      })
    ), 'No se pudieron cargar los álbumes.', selected => {
      if (!selected) return;
      this.album.set(selected.album);
      this.setImages(selected.images);
    });
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
      this.setImages(images);
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
    if (this.photoDateForm.invalid) {
      this.error.set('Revisa la fecha de la foto en las opciones avanzadas.');
      return;
    }
    const date = photoDateInput(this.photoDateForm.getRawValue());
    const invalid = files.find(file => !['image/jpeg', 'image/png'].includes(file.type)
      || file.size > 2 * 1024 * 1024 || file.size === 0);
    if (invalid) {
      this.error.set('Todas las imágenes deben ser JPEG o PNG y pesar hasta 2 MB cada una.');
      return;
    }
    this.photoDateForm.disable();
    const uploads = files.map(file => {
      const body = new FormData();
      body.append('file', file);
      if (date.takenOn) body.append('takenOn', date.takenOn);
      if (date.takenYear !== null) body.append('takenYear', String(date.takenYear));
      return body;
    });
    this.run(from(uploads).pipe(
      concatMap(body => this.http.post<AlbumImage>(this.imagesUrl(album.id), body)),
      toArray(),
      switchMap(() => this.http.get<AlbumImage[]>(this.imagesUrl(album.id))),
      finalize(() => this.photoDateForm.enable())),
    files.length === 1 ? 'No se pudo subir la imagen.' : 'No se pudieron subir las imágenes.', images => {
      this.setImages(images);
      this.photoDateForm.reset();
      this.albums.update(items => items?.map(item => item.id === album.id
        ? { ...item, imageCount: images.length, coverImageId: images[0]?.id ?? null } : item) ?? null);
      this.message.set(files.length === 1
        ? 'Imagen añadida al álbum.'
        : `${files.length} imágenes añadidas al álbum.`);
    });
  }

  protected closeAlbum(): void {

    this.photoDateForm.reset();
    this.album.set(null);
    this.images.set([]);
    if (this.route.snapshot.queryParamMap.has('album')) {
      void this.router.navigate([], {
        relativeTo: this.route, replaceUrl: true, queryParams: { album: null }, queryParamsHandling: 'merge'
      });
    }
  }

  protected view(image: AlbumImage): void {
    const selected = this.album();
    const available: GalleryDialogImage[] = this.images().map(item => ({ ...item }));
    const index = available.findIndex(item => item.id === image.id);
    if (!selected || index < 0) return;
    const dialogRef = this.dialog.open(GalleryImageDialog, {
      data: {
        groupId: this.groupId(), albumTitle: selected.title, images: available, initialIndex: index,
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
    dialogRef.componentInstance.dateChanged.subscribe(({ imageId, date }) => {
      if (this.album()?.id !== selected.id) return;
      this.images.update(images => images.map(image => image.id === imageId ? { ...image, date } : image));
    });
  }

  protected imageLikeUrl(imageId: number): string {
    return `${this.imagesUrl(this.album()!.id)}/${imageId}/like`;
  }

  protected updateLikes(imageId: number, likes: GalleryImageLikes): void {
    this.images.update(images => images.map(image =>
      image.id === imageId ? { ...image, ...likes } : image));
  }

  private setAlbums(albums: Album[]): void { this.albums.set(albums); this.albumLimit.set(12); }

  private setImages(images: AlbumImage[]): void { this.images.set(images); this.imageLimit.set(24); }

  protected thumbnailUrl(albumId: number, imageId: number): string {
    return this.imagesUrl(albumId) + '/' + imageId + '/thumbnail';
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

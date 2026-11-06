import { HttpClient } from '@angular/common/http';
import { Component, DestroyRef, inject, input, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { finalize, Observable, switchMap } from 'rxjs';

import { getApiErrorMessage } from '../../models/api-error.model';

interface Album {
  id: number;
  title: string;
  description: string;
}

interface AlbumImage {
  id: number;
  name: string;
}

@Component({
  selector: 'app-community-gallery',
  imports: [ReactiveFormsModule, MatButtonModule, MatCardModule, MatFormFieldModule, MatInputModule],
  templateUrl: './community-gallery.html',
  styles: [`
    :host { display: block; margin-bottom: 1rem; }
    form { display: grid; gap: .5rem; }
    li { margin-block: .35rem; }
    img { display: block; max-width: 100%; max-height: 32rem; object-fit: contain; }
    p, figcaption, li { overflow-wrap: anywhere; white-space: pre-wrap; }
  `]
})
export class CommunityGallery implements OnInit {
  readonly groupId = input.required<number>();
  readonly canManage = input(false);
  private readonly http = inject(HttpClient);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly albums = signal<Album[] | null>(null);
  protected readonly album = signal<Album | null>(null);
  protected readonly images = signal<AlbumImage[]>([]);
  protected readonly preview = signal<{ url: string; name: string } | null>(null);
  protected readonly busy = signal(false);
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
    this.destroyRef.onDestroy(() => this.clearPreview());
  }

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.run(this.http.get<Album[]>(this.url()), 'No se pudieron cargar los álbumes.',
      albums => this.albums.set(albums));
  }

  protected create(): void {
    if (this.form.invalid || !this.form.controls.title.value.trim()) return;
    this.run(this.http.post<void>(this.url(), this.form.getRawValue()).pipe(switchMap(() => {
      this.form.reset();
      this.creating.set(false);
      this.message.set('Álbum creado.');
      return this.http.get<Album[]>(this.url());
    })), 'No se pudo completar la actualización de los álbumes.', albums => this.albums.set(albums));
  }

  protected open(album: Album): void {
    this.run(this.http.get<AlbumImage[]>(this.imagesUrl(album.id)), 'No se pudo abrir el álbum.', images => {
      this.clearPreview();
      this.album.set(album);
      this.images.set(images);
    });
  }

  protected upload(input: HTMLInputElement, album: Album): void {
    const file = input.files?.item(0);
    input.value = '';
    if (!file || this.busy()) return;
    if (!['image/jpeg', 'image/png'].includes(file.type) || file.size > 2 * 1024 * 1024 || file.size === 0) {
      this.error.set('Selecciona una imagen JPEG o PNG de hasta 2 MB.');
      return;
    }
    const body = new FormData();
    body.append('file', file);
    this.run(this.http.post<AlbumImage>(this.imagesUrl(album.id), body), 'No se pudo subir la imagen.', image => {
      this.images.update(images => [image, ...images]);
      this.message.set('Imagen añadida al álbum.');
    });
  }

  protected view(album: Album, image: AlbumImage): void {
    this.run(this.http.get(`${this.imagesUrl(album.id)}/${image.id}`, { responseType: 'blob' }),
      'No se pudo cargar la imagen.', blob => {
        this.clearPreview();
        this.preview.set({ url: URL.createObjectURL(blob), name: image.name });
      });
  }

  protected clearPreview(): void {
    const preview = this.preview();
    if (preview) URL.revokeObjectURL(preview.url);
    this.preview.set(null);
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
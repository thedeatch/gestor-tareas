import { DatePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, DestroyRef, OnInit, inject, input, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { Observable, finalize } from 'rxjs';
import { getApiErrorMessage } from '../../models/api-error.model';

interface ImageComment {
  id: number;
  authorId: number;
  authorName: string;
  authorHasAvatar: boolean;
  content: string;
  createdAt: string;
  likeCount: number;
  liked: boolean;
}

@Component({
  selector: 'app-gallery-image-comments',
  imports: [DatePipe, ReactiveFormsModule, RouterLink, MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule],
  template: `
    <section aria-label="Comentarios de la foto">
      <div class="heading">
        <h2>Comentarios</h2>
        <button mat-icon-button type="button" aria-label="Actualizar comentarios"
          [disabled]="busy()" (click)="load()"><mat-icon>refresh</mat-icon></button>
      </div>
      @if (error()) { <p role="alert">{{ error() }}</p> }
      @if (comments(); as items) {
        <div class="comments">
          @for (comment of items; track comment.id) {
            <article>
              <header>
                <a class="avatar-link" [routerLink]="['/usuarios', comment.authorId]"
                  [attr.aria-label]="'Ver perfil de ' + comment.authorName">
                  @if (avatarUrls()[comment.authorId]; as avatarUrl) {
                    <img [src]="avatarUrl" alt="" loading="lazy">
                  } @else {
                    <span class="avatar" aria-hidden="true">{{ comment.authorName.charAt(0).toUpperCase() }}</span>
                  }
                </a>
                <div>
                  <a class="author-link" [routerLink]="['/usuarios', comment.authorId]">
                    <strong>{{ comment.authorName }}</strong>
                  </a>
                  <time [attr.datetime]="comment.createdAt">{{ comment.createdAt | date:'dd/MM/yyyy HH:mm' }}</time>
                </div>
              </header>
              <p>{{ comment.content }}</p>
              <button mat-button type="button" [attr.aria-pressed]="comment.liked"
                [attr.aria-label]="'Me gusta, ' + comment.likeCount + ' reacciones'"
                [disabled]="busy()" (click)="toggleLike(comment)">
                <mat-icon>{{ comment.liked ? 'favorite' : 'favorite_border' }}</mat-icon>
                {{ comment.likeCount }}
              </button>
            </article>
          } @empty {
            <p>Aún no hay comentarios.</p>
          }
        </div>
        <form (submit)="save($event)">
          <mat-form-field appearance="outline" subscriptSizing="dynamic">
            <mat-label>Escribe un comentario</mat-label>
            <textarea matInput rows="2" maxlength="2000" [formControl]="content" [readOnly]="busy()"></textarea>
          </mat-form-field>
          <button mat-flat-button type="submit" [disabled]="busy() || !content.value.trim()">
            {{ posting() ? 'Publicando…' : 'Comentar' }}
          </button>
        </form>
      } @else if (busy()) {
        <p role="status">Cargando comentarios…</p>
      } @else {
        <button mat-button type="button" (click)="load()">Reintentar</button>
      }
    </section>
  `,
  styles: [`
    :host { display: block; min-width: 0; }
    h2 { margin: 0 0 .5rem; font-size: 1rem; }
    .heading { display: flex; align-items: center; justify-content: space-between; gap: .5rem; }
    .comments { max-height: 46vh; overflow-y: auto; }
    article { border-bottom: 1px solid var(--mat-sys-outline-variant); padding: .65rem 0; }
    header { display: flex; align-items: center; gap: .5rem; overflow-wrap: anywhere; }
    .avatar-link { display: flex; flex: none; border-radius: 50%; }
    .author-link, .avatar-link { color: inherit; text-decoration: none; }
    .author-link:hover { text-decoration: underline; }
    a:focus-visible { outline: 2px solid var(--mat-sys-primary); outline-offset: 3px; }
    img, .avatar { width: 32px; height: 32px; border-radius: 50%; flex: none; }
    img { object-fit: cover; }
    .avatar { display: grid; place-items: center; background: var(--mat-sys-surface-container-high); }
    time { display: block; font-size: .8rem; color: var(--mat-sys-on-surface-variant); }
    article p { margin: .5rem 0 0; white-space: pre-wrap; overflow-wrap: anywhere; }
    button[aria-pressed="true"] { color: var(--mat-sys-primary); }
    form { display: grid; justify-items: end; gap: .5rem; margin: .75rem 0 .25rem; }
    mat-form-field { width: 100%; }
  `]
})
export class GalleryImageComments implements OnInit {
  readonly url = input.required<string>();
  readonly commentCountChanged = output<number>();
  private readonly http = inject(HttpClient);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly comments = signal<ImageComment[] | null>(null);
  protected readonly avatarUrls = signal<Record<number, string | null>>({});
  protected readonly busy = signal(false);
  protected readonly posting = signal(false);
  protected readonly error = signal('');
  protected readonly content = new FormControl('', { nonNullable: true });

  constructor() {
    this.destroyRef.onDestroy(() => {
      for (const url of Object.values(this.avatarUrls())) {
        if (url) URL.revokeObjectURL(url);
      }
    });
  }

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.run(this.http.get<ImageComment[]>(this.url()), items => {
      this.comments.set([...items].reverse());
      this.commentCountChanged.emit(items.length);
      this.loadAvatars(items);
    },
      'No se pudieron cargar los comentarios.');
  }

  protected save(event: Event): void {
    event.preventDefault();
    const content = this.content.value.trim();
    if (this.busy() || !content || content.length > 2000) return;
    this.posting.set(true);
    this.run(this.http.post<ImageComment>(this.url(), { content }).pipe(
      finalize(() => this.posting.set(false))
    ), saved => {
      this.comments.update(items => [saved, ...(items ?? [])]);
      this.commentCountChanged.emit(this.comments()!.length);
      this.loadAvatars([saved]);
      this.content.reset();
    }, 'No se pudo publicar el comentario.');
  }

  protected toggleLike(comment: ImageComment): void {
    const url = `${this.url()}/${comment.id}/like`;
    const request = comment.liked
      ? this.http.delete<ImageComment>(url)
      : this.http.put<ImageComment>(url, null);
    this.run(request, saved => {
      this.comments.update(items => items?.map(item => item.id === saved.id ? saved : item) ?? null);
    }, 'No se pudo guardar la reacción.');
  }

  private loadAvatars(comments: ImageComment[]): void {
    for (const comment of comments) {
      const id = comment.authorId;
      if (!comment.authorHasAvatar || this.avatarUrls()[id] !== undefined) continue;
      this.avatarUrls.update(urls => ({ ...urls, [id]: null }));
      this.http.get(`/api/users/${id}/avatar`, { responseType: 'blob' })
        .pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
          next: blob => this.avatarUrls.update(urls => ({ ...urls, [id]: URL.createObjectURL(blob) })),
          error: () => {}
        });
    }
  }

  private run<T>(request: Observable<T>, success: (value: T) => void, fallback: string): void {
    if (this.busy()) return;
    this.busy.set(true);
    this.error.set('');
    request.pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.busy.set(false))).subscribe({
      next: success,
      error: error => this.error.set(getApiErrorMessage(error, fallback))
    });
  }
}

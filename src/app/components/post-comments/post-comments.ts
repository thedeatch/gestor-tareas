import { DatePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Observable, finalize } from 'rxjs';
import { confirmCommunityRemoval } from '../admin-reason-dialog/community-removal';
import { ReportButton } from '../community-reports/report-button';
import { ConfirmDialog, ConfirmDialogData } from '../confirm-dialog/confirm-dialog';

interface PostComment {
  id: number;
  authorId: number;
  authorName: string;
  authorHasAvatar: boolean;
  parentId: number | null;
  content: string;
  createdAt: string;
  updatedAt: string;
  editable: boolean;
  deletable: boolean;
}

@Component({
  selector: 'app-post-comments',
  imports: [DatePipe, ReactiveFormsModule, MatButtonModule, MatDialogModule,
    MatFormFieldModule, MatInputModule, ReportButton],
  templateUrl: './post-comments.html',
  styles: [`
    :host { display: block; }
    form, mat-form-field { width: 100%; }
    article { border-top: 1px solid var(--mat-sys-outline-variant); padding-block: .75rem; }
    article p { white-space: pre-wrap; overflow-wrap: anywhere; }
    header { display: flex; align-items: center; flex-wrap: wrap; gap: .5rem; }
    header img { border-radius: 50%; width: 24px; height: 24px; object-fit: cover; }
  `]
})
export class PostComments {
  readonly groupId = input.required<number>();
  readonly postId = input.required<number>();
  readonly rules = input('');
  private readonly http = inject(HttpClient);
  private readonly dialog = inject(MatDialog);
  private readonly url = computed(() =>
    `/api/communities/${this.groupId()}/posts/${this.postId()}/comments`);
  protected readonly open = signal(false);
  protected readonly loaded = signal(false);
  protected readonly busy = signal(false);
  protected readonly error = signal('');
  protected readonly comments = signal<PostComment[]>([]);
  protected readonly authors = computed(() =>
    new Map(this.comments().map(comment => [comment.id, comment.authorName])));
  protected readonly edit = signal<PostComment | null>(null);
  protected readonly reply = signal<PostComment | null>(null);
  protected readonly content = new FormControl('', { nonNullable: true });

  private readonly query = toSignal(inject(ActivatedRoute).queryParamMap);

  constructor() {
    effect(() => {
      if (this.query()?.get('comentarios') !== String(this.postId())) return;
      untracked(() => {
        this.open.set(true);
        if (!this.loaded()) this.load();
      });
    });
  }

  protected toggle(): void {
    this.open.update(open => !open);
    if (this.open() && !this.loaded()) this.load();
  }

  protected load(): void {
    this.run(this.http.get<PostComment[]>(this.url()), comments => {
      this.comments.set(comments);
      this.loaded.set(true);
    }, 'No se pudieron cargar los comentarios.');
  }

  protected beginEdit(comment: PostComment): void {
    this.reply.set(null);
    this.edit.set(comment);
    this.content.setValue(comment.content);
  }

  protected beginReply(comment: PostComment): void {
    this.reset();
    this.reply.set(comment);
  }

  protected reset(): void {
    this.edit.set(null);
    this.reply.set(null);
    this.content.reset();
  }

  protected save(event: Event): void {
    event.preventDefault();
    const content = this.content.value.trim();
    if (this.busy() || !content || content.length > 2000) return;
    const edit = this.edit();
    const request = edit
      ? this.http.patch<PostComment>(`${this.url()}/${edit.id}`, { content })
      : this.http.post<PostComment>(this.url(), { content, parentId: this.reply()?.id });
    this.run(request, saved => {
      this.comments.update(items => edit
        ? items.map(item => item.id === saved.id ? saved : item)
        : [...items, saved]);
      this.reset();
    }, 'No se pudo guardar el comentario.');
  }

  protected confirmDelete(comment: PostComment): void {
    const data: ConfirmDialogData = {
      title: 'Eliminar comentario',
      itemLabel: 'el comentario de',
      itemName: comment.authorName,
      confirmLabel: 'Eliminar'
    };
    const confirmation: Observable<boolean | string | undefined> = comment.editable
      ? this.dialog.open<ConfirmDialog, ConfirmDialogData, boolean>(
        ConfirmDialog, { data, width: '420px' }).afterClosed()
      : confirmCommunityRemoval(this.dialog, 'comentario', comment.authorName);
    confirmation.subscribe(confirmed => {
      if (!confirmed) return;
      const reason = typeof confirmed === 'string' ? confirmed : undefined;
      this.run(this.http.delete<void>(`${this.url()}/${comment.id}`, { body: reason ? { reason } : undefined }), () => {
        this.comments.update(items => items.filter(item => item.id !== comment.id));
        if (this.edit()?.id === comment.id || this.reply()?.id === comment.id) this.reset();
      }, 'No se pudo eliminar el comentario.');
    });
  }

  private run<T>(request: Observable<T>, success: (value: T) => void, message: string): void {
    if (this.busy()) return;
    this.error.set('');
    this.busy.set(true);
    this.content.disable();
    request.pipe(finalize(() => {
      this.busy.set(false);
      this.content.enable();
    })).subscribe({ next: success, error: () => this.error.set(message) });
  }
}
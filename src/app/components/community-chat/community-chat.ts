import { DatePipe } from '@angular/common';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { afterNextRender, Component, DestroyRef, ElementRef, inject, Injector, input, OnInit, output, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { defer, finalize, repeat, retry, Subscription, throwError, timer } from 'rxjs';

import { getApiErrorMessage } from '../../models/api-error.model';

interface ChatMessage {
  id: number;
  authorName: string | null;
  content: string;
  createdAt: string;
}

@Component({
  selector: 'app-community-chat',
  imports: [DatePipe, ReactiveFormsModule, MatButtonModule, MatCardModule, MatFormFieldModule, MatInputModule],
  templateUrl: './community-chat.html',
  styles: [`
    :host { display: block; margin-bottom: 1rem; }
    mat-form-field { width: 100%; }
    ol { max-height: 24rem; overflow-y: auto; padding-inline-start: 1.5rem; }
    li p { white-space: pre-wrap; overflow-wrap: anywhere; margin-top: .25rem; }
    time { margin-inline-start: .5rem; }
  `]
})
export class CommunityChat implements OnInit {
  readonly endpoint = input.required<string>();
  readonly title = input('Chat del grupo');
  readonly autoOpen = input(false);
  readonly messageSent = output<void>();
  private readonly http = inject(HttpClient);
  private readonly destroyRef = inject(DestroyRef);
  private readonly injector = inject(Injector);
  private readonly log = viewChild<ElementRef<HTMLElement>>('log');
  private connection?: Subscription;
  private cursor: number | null = null;
  protected readonly open = signal(false);
  protected readonly connected = signal(false);
  protected readonly reconnecting = signal(false);
  protected readonly messages = signal<ChatMessage[]>([]);
  protected readonly sending = signal(false);
  protected readonly error = signal('');
  protected readonly sendError = signal('');
  protected readonly content = new FormControl('', { nonNullable: true });

  ngOnInit(): void {
    if (this.autoOpen()) this.toggle();
  }

  protected toggle(): void {
    if (this.open()) {
      this.connection?.unsubscribe();
      this.open.set(false);
      return;
    }
    this.messages.set([]);
    this.cursor = null;
    this.open.set(true);
    this.connect();
  }

  protected connect(): void {
    this.connection?.unsubscribe();
    this.connected.set(false);
    this.reconnecting.set(false);
    this.error.set('');
    this.connection = defer(() => {
      const params = this.cursor === null ? new HttpParams() : new HttpParams().set('after', this.cursor);
      return this.http.get<ChatMessage[]>(this.endpoint(), { params });
    }).pipe(
      retry({ resetOnSuccess: true, delay: error => {
        if (error instanceof HttpErrorResponse && [401, 403, 404].includes(error.status)) {
          return throwError(() => error);
        }
        this.reconnecting.set(true);
        return timer(3000);
      } }),
      repeat(),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: messages => {
        this.connected.set(true);
        this.reconnecting.set(false);
        this.cursor = messages.at(-1)?.id ?? this.cursor ?? 0;
        if (!messages.length) return;
        const log = this.log()?.nativeElement;
        const atBottom = !log || log.scrollHeight - log.scrollTop - log.clientHeight < 48;
        this.messages.update(history => [...history, ...messages].slice(-100));
        if (atBottom) {
          afterNextRender(() => {
            const current = this.log()?.nativeElement;
            if (current) current.scrollTop = current.scrollHeight;
          }, { injector: this.injector });
        }
      },
      error: error => {
        this.connected.set(false);
        this.reconnecting.set(false);
        this.messages.set([]);
        this.cursor = null;
        this.error.set(getApiErrorMessage(error, 'No se pudo conectar al chat.'));
      }
    });
  }

  protected send(event: Event): void {
    event.preventDefault();
    const content = this.content.value.trim();
    if (!content || content.length > 1000 || this.sending() || !this.connected()) return;
    this.sending.set(true);
    this.sendError.set('');
    this.http.post<void>(this.endpoint(), { content }).pipe(
      takeUntilDestroyed(this.destroyRef), finalize(() => this.sending.set(false))
    ).subscribe({
      next: () => {
        this.content.reset();
        this.messageSent.emit();
      },
      error: error => this.sendError.set(getApiErrorMessage(error, 'No se pudo enviar el mensaje.'))
    });
  }

}
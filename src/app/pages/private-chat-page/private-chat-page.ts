import { DatePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { finalize } from 'rxjs';

import { CommunityChat } from '../../components/community-chat/community-chat';
import { getApiErrorMessage } from '../../models/api-error.model';

interface Conversation {
  userId: number;
  name: string;
  lastMessageAt: string;
}

@Component({
  selector: 'app-private-chat-page',
  imports: [DatePipe, RouterLink, MatButtonModule, MatCardModule, CommunityChat],
  templateUrl: './private-chat-page.html',
  styles: [`
    main { max-width: 56rem; margin: 1rem auto; padding-inline: 1rem; }
    mat-card { margin-bottom: 1rem; }
    li { overflow-wrap: anywhere; }
  `]
})
export class PrivateChatPage {
  private readonly http = inject(HttpClient);
  private readonly destroyRef = inject(DestroyRef);
  private readonly params = toSignal(inject(ActivatedRoute).queryParamMap);
  protected readonly conversations = signal<Conversation[]>([]);
  protected readonly loading = signal(false);
  protected readonly error = signal('');
  protected readonly selection = computed(() => {
    const id = Number(this.params()?.get('usuario'));
    return Number.isSafeInteger(id) && id > 0 ? [id] : [];
  });

  constructor() {
    this.load();
  }

  protected load(): void {
    if (this.loading()) return;
    this.loading.set(true);
    this.error.set('');
    this.http.get<Conversation[]>('/api/private-chat')
      .pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.loading.set(false))).subscribe({
        next: conversations => this.conversations.set(conversations),
        error: error => this.error.set(getApiErrorMessage(error, 'No se pudieron cargar las conversaciones.'))
      });
  }

  protected title(id: number): string {
    const peer = this.conversations().find(item => item.userId === id);
    return peer ? `Conversación con ${peer.name}` : 'Conversación privada';
  }
}
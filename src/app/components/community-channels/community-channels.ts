import { HttpClient } from '@angular/common/http';
import { Component, DestroyRef, computed, inject, input, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Observable, finalize, switchMap } from 'rxjs';

import { CommunityChat } from '../community-chat/community-chat';
import { getApiErrorMessage } from '../../models/api-error.model';

interface Channel {
  id: number;
  name: string;
}

@Component({
  selector: 'app-community-channels',
  imports: [CommunityChat, ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule],
  templateUrl: './community-channels.html',
  styles: [`
    nav, form { display: flex; flex-wrap: wrap; gap: .5rem; align-items: baseline; margin-bottom: .5rem; }
    button[aria-pressed=true] {
      background: var(--mat-sys-secondary-container); color: var(--mat-sys-on-secondary-container);
    }
  `]
})
export class CommunityChannels implements OnInit {
  readonly groupId = input.required<number>();
  readonly canManage = input(false);
  private readonly http = inject(HttpClient);
  private readonly destroyRef = inject(DestroyRef);
  private readonly base = computed(() => `/api/communities/${this.groupId()}/chat`);
  protected readonly channels = signal<Channel[]>([{ id: 0, name: 'General' }]);
  protected readonly selected = signal(0);
  protected readonly openOnSelect = signal(false);
  protected readonly selection = computed(() => this.channels().filter(channel => channel.id === this.selected()));
  protected readonly busy = signal(false);
  protected readonly creating = signal(false);
  protected readonly error = signal('');
  protected readonly name = new FormControl('', {
    nonNullable: true, validators: [Validators.required, Validators.maxLength(60)]
  });

  ngOnInit(): void {
    this.load();
  }

  protected select(channel: Channel): void {
    this.openOnSelect.set(true);
    this.selected.set(channel.id);
  }

  protected endpoint(channel: Channel): string {
    return channel.id === 0 ? this.base() : `${this.base()}/channels/${channel.id}`;
  }

  protected load(): void {
    this.run(this.fetchChannels(), 'No se pudieron cargar los canales.');
  }

  protected create(event: Event): void {
    event.preventDefault();
    const name = this.name.value.trim();
    if (!name || this.name.invalid) return;

    this.run(this.http.post<void>(`${this.base()}/channels`, { name }).pipe(switchMap(() => {
      this.name.reset();
      this.creating.set(false);
      return this.fetchChannels();
    })), 'No se pudo completar la creación del canal.');
  }

  private fetchChannels() {
    return this.http.get<Channel[]>(`${this.base()}/channels`);
  }

  private run(request: Observable<Channel[]>, fallback: string): void {
    if (this.busy()) return;
    this.busy.set(true);
    this.error.set('');
    request.pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.busy.set(false))).subscribe({
      next: channels => this.channels.set([{ id: 0, name: 'General' }, ...channels]),
      error: error => this.error.set(getApiErrorMessage(error, fallback))
    });
  }
}
import { DatePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, DestroyRef, computed, inject, input, OnInit, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { Observable, Subscription, finalize, tap } from 'rxjs';
import { ListPage } from '../../models/list-page.model';
import { ListPager } from '../list-pager/list-pager';

import { getApiErrorMessage } from '../../models/api-error.model';
import { SearchTarget } from '../../directives/search-target';
import { SaveItemButton } from '../save-item-button/save-item-button';

interface CommunityEvent {
  id: number;
  title: string;
  description: string;
  startsAt: string;
  participantCount: number;
  registered: boolean;
}

@Component({
  selector: 'app-community-events',
  imports: [DatePipe, ReactiveFormsModule, MatButtonModule, MatCardModule,
    MatFormFieldModule, MatInputModule, MatIconModule, SearchTarget, SaveItemButton, ListPager],
  templateUrl: './community-events.html',
  styleUrl: './community-events.css'
})
export class CommunityEvents implements OnInit {
  readonly groupId = input.required<number>();
  readonly canCreate = input(false);
  readonly eventCreated = output<void>();
  private readonly http = inject(HttpClient);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  private request?: Subscription;
  private readonly url = computed(() => `/api/communities/${this.groupId()}/events`);
  protected readonly events = signal<CommunityEvent[] | null>(null);
  protected readonly page = signal(0);
  protected readonly total = signal(0);
  protected readonly busy = signal(false);
  protected readonly creating = signal(false);
  protected readonly error = signal('');
  protected readonly message = signal('');
  protected readonly form = new FormGroup({
    title: new FormControl('', {
      nonNullable: true, validators: [Validators.required, Validators.maxLength(120)]
    }),
    description: new FormControl('', {
      nonNullable: true, validators: [Validators.required, Validators.maxLength(2000)]
    }),
    startsAt: new FormControl('', { nonNullable: true, validators: [Validators.required] })
  });

  ngOnInit(): void {
    const target = Number(this.route.snapshot.queryParamMap.get('evento'));
    this.load(0, Number.isSafeInteger(target) && target > 0 ? target : undefined);
  }

  protected load(page = this.page(), target?: number): void {
    this.request?.unsubscribe();
    this.run(this.http.get<ListPage<CommunityEvent>>(this.url() + '/page', { params: target ? { page, target } : { page } }).pipe(
      tap(result => { this.events.set(result.items); this.page.set(result.page); this.total.set(result.total); })
    ), 'No se pudieron cargar los eventos.');
  }

  protected create(): void {
    if (this.busy() || this.form.invalid) return;
    const { title, description, startsAt } = this.form.getRawValue();
    const date = new Date(startsAt);
    if (!title.trim() || !description.trim() || !(date.getTime() > Date.now())) {
      this.error.set('Completa el título y la descripción y elige una fecha futura.');
      return;
    }

    this.run(this.http.post<void>(this.url(), {
      title, description, startsAt: date.toISOString()
    }), 'No se pudo crear el evento.', () => {
      this.form.reset();
      this.creating.set(false);
      this.message.set('Evento creado.');
      this.eventCreated.emit();
      this.load(0);
    });
  }

  protected started(event: CommunityEvent): boolean {
    return Date.parse(event.startsAt) <= Date.now();
  }

  protected toggleRegistration(event: CommunityEvent): void {
    const url = `${this.url()}/${event.id}/registration`;
    const request = event.registered ? this.http.delete<void>(url) : this.http.put<void>(url, {});
    this.run(request, 'No se pudo actualizar la inscripción.', () => {
      this.message.set(event.registered ? 'Inscripción cancelada.' : 'Inscripción confirmada.');
      this.load();
    });
  }

  private run(request: Observable<unknown>, fallback: string, success?: () => void): void {
    if (this.busy()) return;
    this.error.set('');
    if (success) this.message.set('');
    this.busy.set(true);
    let completed = false;
    this.request = request.pipe(takeUntilDestroyed(this.destroyRef), finalize(() => {
      this.busy.set(false);
      if (completed) success?.();
    })).subscribe({
      error: error => this.error.set(getApiErrorMessage(error, fallback)),
      complete: () => { completed = true; }
    });
  }
}

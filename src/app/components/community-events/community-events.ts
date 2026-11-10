import { DatePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, computed, inject, input, OnInit, output, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { Observable, finalize, tap } from 'rxjs';

import { getApiErrorMessage } from '../../models/api-error.model';

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
    MatFormFieldModule, MatInputModule, MatIconModule],
  templateUrl: './community-events.html',
  styleUrl: './community-events.css'
})
export class CommunityEvents implements OnInit {
  readonly groupId = input.required<number>();
  readonly canCreate = input(false);
  readonly eventCreated = output<void>();
  private readonly http = inject(HttpClient);
  private readonly url = computed(() => `/api/communities/${this.groupId()}/events`);
  protected readonly events = signal<CommunityEvent[] | null>(null);
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
    this.load();
  }

  protected load(): void {
    this.run(this.http.get<CommunityEvent[]>(this.url()).pipe(
      tap(events => this.events.set(events))
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
      this.load();
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
    request.pipe(finalize(() => {
      this.busy.set(false);
      if (completed) success?.();
    })).subscribe({
      error: error => this.error.set(getApiErrorMessage(error, fallback)),
      complete: () => { completed = true; }
    });
  }
}

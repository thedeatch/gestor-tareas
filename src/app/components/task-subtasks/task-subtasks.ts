import { HttpClient } from '@angular/common/http';
import { Component, DestroyRef, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { Observable, finalize, switchMap, tap } from 'rxjs';

import { getApiErrorMessage } from '../../models/api-error.model';

interface Subtask {
  id: number;
  description: string;
  completed: boolean;
}

@Component({
  selector: 'app-task-subtasks',
  imports: [FormsModule, MatButtonModule],
  templateUrl: './task-subtasks.html',
  styles: [`
    :host { display: block; margin-inline-start: .75rem; }
    form, li { display: flex; flex-wrap: wrap; align-items: center; gap: .5rem; margin-block: .4rem; }
    input[type=text] { flex: 1; min-width: 8rem; padding: .4rem; }
    label { overflow-wrap: anywhere; }
  `]
})
export class TaskSubtasks {
  readonly endpoint = input.required<string>();
  readonly editable = input(true);
  private readonly http = inject(HttpClient);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly open = signal(false);
  protected readonly items = signal<Subtask[] | null>(null);
  protected readonly busy = signal(false);
  protected readonly error = signal('');
  protected readonly editing = signal<Subtask | null>(null);
  protected readonly completed = computed(() => this.items()?.filter(item => item.completed).length ?? 0);
  protected description = '';

  protected toggle(): void {
    this.open.update(value => !value);
    if (this.open()) this.load();
  }

  protected load(): void {
    this.run(this.fetch(), 'No se pudieron cargar las subtareas.');
  }

  protected edit(item: Subtask): void {
    this.editing.set(item);
    this.description = item.description;
  }

  protected reset(): void {
    this.editing.set(null);
    this.description = '';
  }

  protected save(): void {
    const description = this.description.trim();
    if (!description || description.length > 150) return;
    const item = this.editing();
    const request = item
      ? this.http.put<void>(`${this.endpoint()}/${item.id}`, { description, completed: item.completed })
      : this.http.post<void>(this.endpoint(), { description });
    this.mutate(request.pipe(tap(() => this.reset())));
  }

  protected check(item: Subtask, control: HTMLInputElement): void {
    const completed = control.checked;
    control.checked = item.completed;
    this.mutate(this.http.put<void>(`${this.endpoint()}/${item.id}`, {
      description: item.description, completed
    }));
  }

  protected remove(item: Subtask): void {
    this.mutate(this.http.delete<void>(`${this.endpoint()}/${item.id}`).pipe(tap(() => {
      if (this.editing()?.id === item.id) this.reset();
    })));
  }

  private fetch() {
    return this.http.get<Subtask[]>(this.endpoint());
  }

  private mutate(request: Observable<void>): void {
    this.run(request.pipe(switchMap(() => this.fetch())), 'No se pudo completar el cambio.');
  }

  private run(request: Observable<Subtask[]>, fallback: string): void {
    if (this.busy()) return;
    this.busy.set(true);
    this.error.set('');
    request.pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.busy.set(false))).subscribe({
      next: items => this.items.set(items),
      error: error => this.error.set(getApiErrorMessage(error, fallback))
    });
  }
}
import { HttpClient } from '@angular/common/http';
import { Component, DestroyRef, computed, effect, inject, signal, untracked } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { Observable, finalize, switchMap, tap } from 'rxjs';

import { Task } from '../../models/task.model';
import { getApiErrorMessage } from '../../models/api-error.model';
import { TaskService } from '../../services/task';
import { ConfirmDialog, ConfirmDialogData } from '../confirm-dialog/confirm-dialog';

type Kind = 'PROJECT' | 'CATEGORY';
interface Item {
  id: number;
  kind: Kind;
  name: string;
}

@Component({
  selector: 'app-task-organization',
  imports: [FormsModule, MatButtonModule, MatDialogModule],
  templateUrl: './task-organization.html',
  styles: [`
    :host { display: block; margin-block: 1rem; }
    form, .fields { display: flex; flex-wrap: wrap; align-items: end; gap: .75rem; }
    label { display: grid; gap: .25rem; min-width: 0; }
    select, input { max-width: 100%; padding: .4rem; }
    li { overflow-wrap: anywhere; }
  `]
})
export class TaskOrganization {
  private readonly http = inject(HttpClient);
  private readonly dialog = inject(MatDialog);
  private readonly destroyRef = inject(DestroyRef);
  private readonly taskService = inject(TaskService);
  private readonly url = '/api/tasks/organization';
  protected readonly tasks = this.taskService.tasks;
  protected readonly items = signal<Item[] | null>(null);
  protected readonly projects = computed(() => this.items()?.filter(item => item.kind === 'PROJECT') ?? []);
  protected readonly categories = computed(() => this.items()?.filter(item => item.kind === 'CATEGORY') ?? []);
  protected readonly busy = signal(false);
  protected readonly error = signal('');
  protected readonly message = signal('');
  protected readonly kind = signal<Kind>('PROJECT');
  protected readonly name = signal('');
  protected readonly editing = signal<Item | null>(null);
  protected readonly selected = signal<number | null>(null);
  protected readonly project = signal<number | null>(null);
  protected readonly category = signal<number | null>(null);

  constructor() {
    effect(() => {
      if (this.tasks().some(task => task.projectId || task.categoryId)) {
        untracked(() => {
          if (this.items() === null) this.load();
        });
      }
    });
  }

  label(task: Task): string {
    return [task.projectId, task.categoryId]
      .map(id => this.items()?.find(item => item.id === id)?.name)
      .filter(Boolean).join(' · ');
  }

  protected load(): void {
    this.run(this.fetch(), 'No se pudo cargar la organización.', items => this.items.set(items));
  }

  protected edit(item: Item): void {
    this.editing.set(item);
    this.kind.set(item.kind);
    this.name.set(item.name);
  }

  protected reset(): void {
    this.editing.set(null);
    this.name.set('');
  }

  protected save(): void {
    const name = this.name().trim();
    if (!name || name.length > 80) return;
    const item = this.editing();
    const request = item
      ? this.http.patch<void>(`${this.url}/${item.id}`, { name })
      : this.http.post<void>(this.url, { name, kind: this.kind() });
    this.run(request.pipe(tap(() => this.reset()), switchMap(() => this.fetch())),
      'No se pudo guardar el cambio.', items => {
        this.items.set(items);
        this.message.set('Organización actualizada.');
      });
  }

  protected remove(item: Item): void {
    const data: ConfirmDialogData = {
      title: 'Eliminar clasificación',
      itemLabel: item.kind === 'PROJECT' ? 'el proyecto' : 'la categoría',
      itemName: item.name,
      confirmLabel: 'Eliminar'
    };
    this.dialog.open<ConfirmDialog, ConfirmDialogData, boolean>(ConfirmDialog, { data })
      .afterClosed().pipe(takeUntilDestroyed(this.destroyRef)).subscribe(confirmed => {
        if (!confirmed) return;
        this.run(this.http.delete<void>(`${this.url}/${item.id}`), 'No se pudo eliminar.', () => {
          this.items.update(items => items?.filter(current => current.id !== item.id) ?? []);
          this.taskService.removeClassification(item.id);
          this.selectTask(this.selected());
          if (this.editing()?.id === item.id) this.reset();
          this.message.set('Clasificación eliminada. Las tareas se conservaron.');
        });
      });
  }

  protected selectTask(id: number | null): void {
    this.selected.set(id);
    const task = this.tasks().find(item => item.id === id);
    this.project.set(task?.projectId ?? null);
    this.category.set(task?.categoryId ?? null);
  }

  protected classify(): void {
    const id = this.selected();
    if (id === null) return;
    this.run(this.taskService.classifyTask(id, this.project(), this.category()),
      'No se pudo clasificar la tarea.', () => this.message.set('Clasificación guardada.'));
  }

  private fetch() {
    return this.http.get<Item[]>(this.url);
  }

  private run<T>(request: Observable<T>, fallback: string, success: (result: T) => void): void {
    if (this.busy()) return;
    this.busy.set(true);
    this.error.set('');
    this.message.set('');
    request.pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.busy.set(false))).subscribe({
      next: success,
      error: error => this.error.set(getApiErrorMessage(error, fallback))
    });
  }
}
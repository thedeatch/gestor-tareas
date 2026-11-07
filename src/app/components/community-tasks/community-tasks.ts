import { DatePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, DestroyRef, computed, inject, input, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { Observable, finalize, switchMap, tap } from 'rxjs';

import { CommunityMember } from '../../models/community.model';
import { getApiErrorMessage } from '../../models/api-error.model';
import { TASK_PRIORITY_LABELS, TASK_STATUS_LABELS, TaskPriority, TaskStatus } from '../../models/task.model';

import { TaskFilters } from '../task-filters/task-filters';
import { EMPTY_TASK_FILTER, TaskFilter, filterTasks } from '../../models/task-filter.model';
import { TaskSubtasks } from '../task-subtasks/task-subtasks';

interface GroupTask {
  id: number;
  description: string;
  priority: TaskPriority;
  status: TaskStatus;
  dueDate: string | null;
  assigneeId: number | null;
  assigneeName: string | null;
  canChangeStatus: boolean;
}

@Component({
  selector: 'app-community-tasks',
  imports: [DatePipe, FormsModule, MatButtonModule, MatCardModule, TaskSubtasks, TaskFilters],
  templateUrl: './community-tasks.html',
  styles: [`
    :host { display: block; margin-bottom: 1rem; }
    form, .fields { display: flex; flex-wrap: wrap; align-items: end; gap: .75rem; }
    label { display: grid; gap: .25rem; min-width: 0; }
    select, input { max-width: 100%; padding: .4rem; }
    article { padding-block: .75rem; border-top: 1px solid var(--mat-sys-outline-variant); }
    h3 { overflow-wrap: anywhere; }
  `]
})
export class CommunityTasks implements OnInit {
  readonly groupId = input.required<number>();
  readonly members = input.required<CommunityMember[]>();
  readonly canManage = input(false);
  private readonly http = inject(HttpClient);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly tasks = signal<GroupTask[] | null>(null);
  protected readonly filters = signal<TaskFilter>({ ...EMPTY_TASK_FILTER });
  protected readonly filteredTasks = computed(() => filterTasks(this.tasks() ?? [], this.filters()));
  protected readonly assignees = computed(() => {
    const options = new Map(this.members().map(member => [member.userId, { id: member.userId, name: member.name }]));
    for (const task of this.tasks() ?? []) {
      if (task.assigneeId !== null && !options.has(task.assigneeId)) {
        options.set(task.assigneeId, { id: task.assigneeId, name: task.assigneeName ?? 'Miembro anterior' });
      }
    }
    return [...options.values()];
  });
  protected readonly busy = signal(false);
  protected readonly creating = signal(false);
  protected readonly error = signal('');
  protected readonly message = signal('');
  protected readonly priorities = TASK_PRIORITY_LABELS;
  protected readonly statuses = TASK_STATUS_LABELS;
  protected readonly priorityOptions = Object.entries(TASK_PRIORITY_LABELS);
  protected readonly statusOptions = Object.entries(TASK_STATUS_LABELS);
  protected description = '';
  protected priority: TaskPriority = 'NORMAL';
  protected dueDate = '';
  protected assignee = '';

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.run(this.fetch(), 'No se pudieron cargar las tareas del grupo.');
  }

  protected create(): void {
    const description = this.description.trim();
    if (!description || description.length > 150) return;
    this.mutate(this.http.post<void>(this.url(), {
      description, priority: this.priority, dueDate: this.dueDate || null,
      assigneeId: this.assignee ? Number(this.assignee) : null
    }).pipe(tap(() => {
      this.description = '';
      this.priority = 'NORMAL';
      this.dueDate = '';
      this.assignee = '';
      this.creating.set(false);
    })), 'Tarea creada.');
  }

  protected changeAssignment(task: GroupTask, select: HTMLSelectElement): void {
    const assigneeId = select.value ? Number(select.value) : null;
    select.value = String(task.assigneeId ?? '');
    if (assigneeId === task.assigneeId) return;
    this.mutate(this.http.patch<void>(`${this.url()}/${task.id}/assignment`, { assigneeId }),
      'Asignación actualizada.');
  }

  protected changeStatus(task: GroupTask, select: HTMLSelectElement): void {
    const status = select.value;
    select.value = task.status;
    if (status === task.status) return;
    this.mutate(this.http.patch<void>(`${this.url()}/${task.id}/status`, { status }),
      'Estado actualizado.');
  }

  private mutate(request: Observable<void>, message: string): void {
    this.run(request.pipe(tap(() => this.message.set(message)), switchMap(() => this.fetch())),
      'No se pudo completar la actualización.');
  }

  private url(): string {
    return `/api/communities/${this.groupId()}/tasks`;
  }

  private fetch() {
    return this.http.get<GroupTask[]>(this.url());
  }

  private run(request: Observable<GroupTask[]>, fallback: string): void {
    if (this.busy()) return;
    this.busy.set(true);
    this.error.set('');
    this.message.set('');
    request.pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.busy.set(false))).subscribe({
      next: tasks => this.tasks.set(tasks),
      error: error => this.error.set(getApiErrorMessage(error, fallback))
    });
  }
}
import { DatePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, DestroyRef, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { Observable, Subscription, distinctUntilChanged, finalize, map, switchMap, tap } from 'rxjs';
import { ListPage } from '../../models/list-page.model';
import { taskPageParams } from '../../models/task-page-params';
import { ListPager } from '../list-pager/list-pager';

import { CommunityMember } from '../../models/community.model';
import { getApiErrorMessage } from '../../models/api-error.model';
import { TASK_PRIORITY_LABELS, TASK_STATUS_LABELS, TaskPriority, TaskStatus } from '../../models/task.model';

import { TaskFilters } from '../task-filters/task-filters';
import { TaskKanban, TaskMove } from '../task-kanban/task-kanban';
import { TaskCalendar } from '../task-calendar/task-calendar';
import { TaskProductivity } from '../task-productivity/task-productivity';
import { EMPTY_TASK_FILTER, TaskFilter, filterTasks, localDate } from '../../models/task-filter.model';
import { TaskSubtasks } from '../task-subtasks/task-subtasks';
import { TaskDateService } from '../../services/task-date';
import { SearchTarget } from '../../directives/search-target';

import { CommunityTask as GroupTask } from '../../models/community-goal.model';

@Component({
  selector: 'app-community-tasks',
  imports: [DatePipe, FormsModule, MatButtonModule, MatCardModule, TaskSubtasks, TaskFilters, TaskKanban, TaskCalendar, TaskProductivity, SearchTarget, ListPager],
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
export class CommunityTasks {
  readonly groupId = input.required<number>();
  readonly members = input.required<CommunityMember[]>();
  readonly canManage = input(false);
  private readonly http = inject(HttpClient);
  private readonly destroyRef = inject(DestroyRef);
  private readonly taskDate = inject(TaskDateService);
  private readonly route = inject(ActivatedRoute);
  private readonly target = toSignal(this.route.queryParamMap.pipe(map(params => Number(params.get('tarea'))), distinctUntilChanged()), { initialValue: 0 });
  private lastTarget = 0;
  private pendingTarget?: number;
  private reading?: Subscription;
  private mutating = false;
  private pendingRead = false;
  private readonly assignedPeople = signal<{ id: number; name: string }[]>([]);
  protected readonly page = signal<ListPage<GroupTask> | null>(null);
  private pageNumber = 0;
  protected readonly tasks = signal<GroupTask[] | null>(null);
  protected readonly filters = signal<TaskFilter>({ ...EMPTY_TASK_FILTER });
  protected readonly filteredTasks = computed(() => this.view() === 'LIST' ? this.tasks() ?? [] : filterTasks(this.tasks() ?? [], this.filters(), this.taskDate.now()));
  protected readonly assignees = computed(() => {
    const options = new Map(this.members().map(member => [member.userId, { id: member.userId, name: member.name }]));
    for (const person of this.assignedPeople()) options.set(person.id, person);
    for (const task of this.tasks() ?? []) {
      if (task.assigneeId !== null && !options.has(task.assigneeId)) {
        options.set(task.assigneeId, { id: task.assigneeId, name: task.assigneeName ?? 'Miembro anterior' });
      }
    }
    return [...options.values()];
  });
  protected readonly busy = signal(false);
  protected readonly view = signal<'LIST' | 'KANBAN' | 'CALENDAR' | 'STATS'>('LIST');
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

  private readonly today = computed(() => localDate(this.taskDate.now()));

  constructor() {
    effect(onCleanup => {
      const group = this.groupId();
      const request = this.http.get<{ id: number; name: string }[]>(`/api/communities/${group}/tasks/assignees`)
        .pipe(takeUntilDestroyed(this.destroyRef)).subscribe({ next: people => this.assignedPeople.set(people), error: () => this.assignedPeople.set([]) });
      onCleanup(() => request.unsubscribe());
    });
    effect(() => {
      const target = this.target(); this.filters(); this.view(); this.today();
      untracked(() => {
        if (target !== this.lastTarget) {
          this.pendingTarget = Number.isSafeInteger(target) && target > 0 ? target : undefined;
          this.lastTarget = target;
        }
        this.pageNumber = 0; this.load();
      });
    });
  }

  protected load(): void {
    if (this.mutating) { this.pendingRead = true; return; }
    this.reading?.unsubscribe();
    this.reading = this.run(this.fetch(), 'No se pudieron cargar las tareas del grupo.');
  }

  protected changePage(page: number): void { this.pageNumber = page; this.pendingTarget = undefined; this.load(); }
  protected filterChanged(filter: TaskFilter): void { this.pendingTarget = undefined; this.filters.set(filter); }

  protected create(): void {
    const description = this.description.trim();
    if (this.busy() || !description || description.length > 150) return;
    this.pageNumber = 0;
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
    const status = select.value as TaskStatus;
    select.value = task.status;
    if (status === task.status) return;
    this.moveTask({ id: task.id, status });
  }

  protected moveTask(move: TaskMove): void {
    this.mutate(this.http.patch<void>(`${this.url()}/${move.id}/status`, { status: move.status }),
      'Estado actualizado.');
  }

  private mutate(request: Observable<void>, message: string): void {
    if (this.busy()) return;
    this.mutating = true;
    this.run(request.pipe(tap(() => this.message.set(message)), switchMap(() => this.fetch())),
      'No se pudo completar la actualización.', () => {
        this.mutating = false;
        if (this.pendingRead) { this.pendingRead = false; this.load(); }
      });
  }

  private url(): string {
    return `/api/communities/${this.groupId()}/tasks`;
  }

  private fetch() {
    if (this.view() === 'LIST') {
      return this.http.get<ListPage<GroupTask>>(this.url() + '/page', {
        params: taskPageParams(this.filters(), this.pageNumber, this.taskDate.now(), this.pendingTarget)
      }).pipe(tap(result => { this.page.set(result); this.pageNumber = result.page; this.pendingTarget = undefined; }), map(result => result.items));
    }
    this.page.set(null);
    return this.http.get<GroupTask[]>(this.url());
  }

  private run(request: Observable<GroupTask[]>, fallback: string, after?: () => void): Subscription | undefined {
    if (this.busy()) return;
    this.busy.set(true);
    this.error.set('');
    this.message.set('');
    return request.pipe(takeUntilDestroyed(this.destroyRef), finalize(() => { this.busy.set(false); after?.(); })).subscribe({
      next: tasks => this.tasks.set(tasks),
      error: error => this.error.set(getApiErrorMessage(error, fallback))
    });
  }
}

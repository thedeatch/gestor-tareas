import { Component, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';

import { EMPTY_TASK_FILTER, TaskFilter, TaskFilterOption } from '../../models/task-filter.model';
import { TASK_PRIORITY_LABELS, TASK_STATUS_LABELS } from '../../models/task.model';

@Component({
  selector: 'app-task-filters',
  imports: [FormsModule, MatButtonModule],
  templateUrl: './task-filters.html',
  styles: [`
    :host { display: block; margin-block: 1rem; }
    .filters { display: flex; flex-wrap: wrap; gap: .75rem; align-items: end; }
    label { display: grid; gap: .25rem; min-width: 0; }
    select, input { max-width: 100%; padding: .4rem; }
    .search { flex: 1; min-width: min(100%, 12rem); }
  `]
})
export class TaskFilters {
  readonly projects = input<TaskFilterOption[]>([]);
  readonly categories = input<TaskFilterOption[]>([]);
  readonly assignees = input<TaskFilterOption[]>([]);
  readonly personal = input(false);
  readonly changed = output<TaskFilter>();
  protected readonly filter = signal<TaskFilter>({ ...EMPTY_TASK_FILTER });
  protected readonly statuses = Object.entries(TASK_STATUS_LABELS);
  protected readonly priorities = Object.entries(TASK_PRIORITY_LABELS);

  protected update<K extends keyof TaskFilter>(key: K, value: TaskFilter[K]): void {
    const next = { ...this.filter(), [key]: value };
    this.filter.set(next);
    this.changed.emit(next);
  }

  protected clear(): void {
    const next = { ...EMPTY_TASK_FILTER };
    this.filter.set(next);
    this.changed.emit(next);
  }
}
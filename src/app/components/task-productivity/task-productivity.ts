import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, computed, inject, input } from '@angular/core';

import { taskProductivity } from '../../models/task-productivity.model';
import { TASK_PRIORITY_LABELS, TASK_STATUS_LABELS, TaskCardData } from '../../models/task.model';
import { TaskDateService } from '../../services/task-date';

@Component({
  selector: 'app-task-productivity',
  imports: [DatePipe, DecimalPipe],
  templateUrl: './task-productivity.html',
  styleUrl: './task-productivity.css'
})
export class TaskProductivity {
  readonly tasks = input.required<TaskCardData[]>();
  readonly group = input(false);
  protected readonly today = inject(TaskDateService).now;
  protected readonly summary = computed(() => taskProductivity(this.tasks(), this.today()));
  protected readonly statusRows = computed(() => Object.entries(TASK_STATUS_LABELS).map(([key, label]) => ({
    label, count: this.summary().statuses[key as keyof typeof TASK_STATUS_LABELS]
  })));
  protected readonly priorityRows = computed(() => Object.entries(TASK_PRIORITY_LABELS).map(([key, label]) => ({
    label, count: this.summary().priorities[key as keyof typeof TASK_PRIORITY_LABELS]
  })));
}

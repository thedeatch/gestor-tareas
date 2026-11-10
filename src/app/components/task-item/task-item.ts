import { Component, input, output } from '@angular/core';
import { DatePipe } from '@angular/common';
import {
  TASK_PRIORITY_LABELS,
  TASK_STATUS_LABELS,
  Task,
  TaskPriority,
  TaskStatus
} from '../../models/task.model';

import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-task-item',
  host: {
    '[class.priority-high]': "(task().priority ?? 'NORMAL') === 'HIGH'",
    '[class.priority-normal]': "(task().priority ?? 'NORMAL') === 'NORMAL'",
    '[class.priority-low]': "task().priority === 'LOW'"
  },
  imports: [
  DatePipe,
  MatButtonModule,
  MatCheckboxModule,
  MatIconModule
],
  templateUrl: './task-item.html',
  styleUrl: './task-item.css'
})
export class TaskItem {
  readonly task = input.required<Task>();
  protected readonly priorities = TASK_PRIORITY_LABELS;
  protected readonly statuses = TASK_STATUS_LABELS;

  readonly taskToggled = output<number>();
  readonly taskEdited = output<number>();
  readonly taskDeleted = output<number>();

  protected status(task: Task): TaskStatus {
    return task.status ?? (task.completed ? 'COMPLETED' : 'PENDING');
  }

  protected statusIcon(status: TaskStatus): string {
    return { PENDING: 'pending_actions', IN_PROGRESS: 'autorenew',
      COMPLETED: 'task_alt' }[status];
  }

  protected priorityIcon(priority: TaskPriority): string {
    return { LOW: 'south', NORMAL: 'remove', HIGH: 'priority_high' }[priority];
  }

  protected isOverdue(task: Task): boolean {
    if (!task.dueDate || this.status(task) === 'COMPLETED') return false;
    return new Date(`${task.dueDate}T23:59:59`).getTime() < Date.now();
  }
}

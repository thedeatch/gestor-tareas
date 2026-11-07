import { DatePipe } from '@angular/common';
import { CdkDragDrop, DragDropModule } from '@angular/cdk/drag-drop';
import { Component, computed, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';

import {
  TASK_PRIORITY_LABELS, TASK_STATUS_LABELS, TaskStatus,
  TaskCardData as KanbanTask, TaskStatusChange as TaskMove
} from '../../models/task.model';
import { TaskSubtasks } from '../task-subtasks/task-subtasks';

export type { KanbanTask, TaskMove };

@Component({
  selector: 'app-task-kanban',
  imports: [DatePipe, DragDropModule, MatButtonModule, TaskSubtasks],
  templateUrl: './task-kanban.html',
  styleUrl: './task-kanban.css'
})
export class TaskKanban {
  readonly tasks = input.required<KanbanTask[]>();
  readonly busy = input(false);
  readonly taskEndpoint = input.required<string>();
  readonly allowEdit = input(false);
  readonly moved = output<TaskMove>();
  readonly edited = output<number>();
  protected readonly priorities = TASK_PRIORITY_LABELS;
  protected readonly columns = computed(() => {
    const tasks = this.tasks();
    return (Object.entries(TASK_STATUS_LABELS) as [TaskStatus, string][]).map(([status, label]) => ({
      status,
      label,
      tasks: tasks.filter(task => this.statusOf(task) === status)
    }));
  });

  protected drop(event: CdkDragDrop<TaskStatus, TaskStatus, KanbanTask>): void {
    this.move(event.item.data, event.container.data);
  }

  protected changeStatus(task: KanbanTask, select: HTMLSelectElement): void {
    const status = select.value as TaskStatus;
    select.value = this.statusOf(task);
    this.move(task, status);
  }

  private move(task: KanbanTask, status: TaskStatus): void {
    if (this.busy() || task.canChangeStatus === false || status === this.statusOf(task)) return;
    this.moved.emit({ id: task.id, status });
  }

  private statusOf(task: KanbanTask): TaskStatus {
    return task.status ?? (task.completed ? 'COMPLETED' : 'PENDING');
  }
}

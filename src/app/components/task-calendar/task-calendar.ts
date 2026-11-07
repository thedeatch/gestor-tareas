import { DatePipe, NgTemplateOutlet } from '@angular/common';
import { Component, computed, input, output, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';

import {
  TASK_PRIORITY_LABELS, TASK_STATUS_LABELS, TaskCardData, TaskPriority, TaskStatus, TaskStatusChange
} from '../../models/task.model';
import { TaskSubtasks } from '../task-subtasks/task-subtasks';

interface CalendarDay {
  key: string;
  day: number;
  currentMonth: boolean;
  available: boolean;
  tasks: TaskCardData[];
}

// Calendar dates are civil dates. UTC arithmetic keeps them stable across daylight-saving changes.
function calendarDate(year: number, month: number, day: number): Date {
  const date = new Date(0);
  date.setUTCFullYear(year, month, day);
  date.setUTCHours(0, 0, 0, 0);
  return date;
}

function dateKey(date: Date): string {
  return `${String(date.getUTCFullYear()).padStart(4, '0')}-${String(date.getUTCMonth() + 1).padStart(2, '0')}-${String(date.getUTCDate()).padStart(2, '0')}`;
}

function localToday(): string {
  const now = new Date();
  return dateKey(calendarDate(now.getFullYear(), now.getMonth(), now.getDate()));
}

@Component({
  selector: 'app-task-calendar',
  imports: [DatePipe, NgTemplateOutlet, MatButtonModule, TaskSubtasks],
  templateUrl: './task-calendar.html',
  styleUrl: './task-calendar.css'
})
export class TaskCalendar {
  readonly tasks = input.required<TaskCardData[]>();
  readonly taskEndpoint = input.required<string>();
  readonly busy = input(false);
  readonly allowEdit = input(false);
  readonly moved = output<TaskStatusChange>();
  readonly edited = output<number>();
  protected readonly selected = signal(localToday());
  protected readonly month = signal(this.selected().slice(0, 7));
  protected readonly weekdays = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom'];
  protected readonly priorities = TASK_PRIORITY_LABELS;
  protected readonly statuses = TASK_STATUS_LABELS;
  protected readonly statusOptions = Object.entries(TASK_STATUS_LABELS);
  protected readonly monthLabel = computed(() => {
    const [year, month] = this.month().split('-').map(Number);
    return new Intl.DateTimeFormat('es-CL', { month: 'long', year: 'numeric', timeZone: 'UTC' })
      .format(calendarDate(year, month - 1, 1));
  });
  protected readonly days = computed<CalendarDay[]>(() => {
    const [year, month] = this.month().split('-').map(Number);
    const first = calendarDate(year, month - 1, 1);
    const offset = (first.getUTCDay() + 6) % 7;
    const tasksByDate = new Map<string, TaskCardData[]>();
    for (const task of this.tasks()) {
      if (!task.dueDate) continue;
      const items = tasksByDate.get(task.dueDate) ?? [];
      items.push(task);
      tasksByDate.set(task.dueDate, items);
    }
    return Array.from({ length: 42 }, (_, index) => {
      const date = calendarDate(year, month - 1, index - offset + 1);
      const key = dateKey(date);
      return {
        key,
        day: date.getUTCDate(),
        currentMonth: date.getUTCFullYear() === year && date.getUTCMonth() === month - 1,
        available: date.getUTCFullYear() >= 1 && date.getUTCFullYear() <= 9999,
        tasks: tasksByDate.get(key) ?? []
      };
    });
  });
  protected readonly selectedTasks = computed(() => this.tasks().filter(task => task.dueDate === this.selected()));
  protected readonly undated = computed(() => this.tasks().filter(task => !task.dueDate));

  protected moveMonth(delta: number): void {
    const [year, month] = this.month().split('-').map(Number);
    const next = calendarDate(year, month - 1 + delta, 1);
    if (next.getUTCFullYear() < 1 || next.getUTCFullYear() > 9999) return;
    this.select(dateKey(next));
  }

  protected select(key: string): void {
    this.selected.set(key);
    this.month.set(key.slice(0, 7));
  }

  protected today(): string {
    return localToday();
  }

  protected dayLabel(day: CalendarDay): string {
    const [year, month, date] = day.key.split('-');
    return `${date}/${month}/${year} · ${day.tasks.length} tareas`;
  }

  protected statusOf(task: TaskCardData): TaskStatus {
    return task.status ?? (task.completed ? 'COMPLETED' : 'PENDING');
  }

  protected priorityOf(task: TaskCardData): TaskPriority {
    return task.priority ?? 'NORMAL';
  }

  protected changeStatus(task: TaskCardData, select: HTMLSelectElement): void {
    const status = select.value as TaskStatus;
    select.value = this.statusOf(task);
    if (this.busy() || task.canChangeStatus === false || status === this.statusOf(task)) return;
    this.moved.emit({ id: task.id, status });
  }
}

import { Component, input, output } from '@angular/core';
import { DatePipe } from '@angular/common';
import { TASK_PRIORITY_LABELS, TASK_STATUS_LABELS, Task } from '../../models/task.model';

import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-task-item',
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
}
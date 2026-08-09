import { Component, input, output } from '@angular/core';
import { Task } from '../../models/task.model';

import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-task-item',
  imports: [
  MatButtonModule,
  MatCheckboxModule,
  MatIconModule
],
  templateUrl: './task-item.html',
  styleUrl: './task-item.css'
})
export class TaskItem {
  readonly task = input.required<Task>();

  readonly taskToggled = output<number>();
  readonly taskDeleted = output<number>();
}
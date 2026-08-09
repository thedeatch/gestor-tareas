import { Component, input, output } from '@angular/core';
import { Task } from '../../models/task.model';

@Component({
  selector: 'app-task-item',
  imports: [],
  templateUrl: './task-item.html',
  styleUrl: './task-item.css'
})
export class TaskItem {
  readonly task = input.required<Task>();

  readonly taskToggled = output<number>();
  readonly taskDeleted = output<number>();
}
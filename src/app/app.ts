import { Component, inject, signal } from '@angular/core';

import { TaskForm } from './components/task-form/task-form';
import { TaskItem } from './components/task-item/task-item';
import { TaskService } from './services/task';

@Component({
  selector: 'app-root',
  imports: [TaskForm, TaskItem],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('Gestor de tareas');

  private readonly taskService = inject(TaskService);

  protected readonly pendingTasks = this.taskService.pendingTasks;
  protected readonly completedTasks = this.taskService.completedTasks;

  protected addTask(description: string): void {
    this.taskService.addTask(description);
  }

  protected toggleTask(id: number): void {
    this.taskService.toggleTask(id);
  }

  protected deleteTask(id: number): void {
    this.taskService.deleteTask(id);
  }
}
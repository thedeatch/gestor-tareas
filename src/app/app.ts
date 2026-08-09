import { Component, computed, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Task } from './models/task.model';
import { TaskItem } from './components/task-item/task-item';
import { TaskForm } from './components/task-form/task-form';
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, TaskForm, TaskItem],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('Gestor de tareas');
  protected readonly tasks = signal<Task[]>([
    {
      id: 1,
      description: 'Aprender componentes Angular',
      completed: false
    },
    {
      id: 2,
      description: 'Practicar TypeScript',
      completed: false
    },
    {
      id: 3,
      description: 'Instalar Angular',
      completed: true
    }
  ]);

  protected toggleTask(id: number): void {
    this.tasks.update(tasks =>
      tasks.map(task =>
        task.id === id
          ? { ...task, completed: !task.completed }
          : task
      )
    );
  }
  protected addTask(description: string): void {
    this.tasks.update(tasks => [
      ...tasks,
      {
        id: Date.now(),
        description,
        completed: false
      }
    ]);
  }
  protected deleteTask(id: number): void {
    this.tasks.update(tasks =>
      tasks.filter(task => task.id !== id)
    );
  }
  protected readonly pendingTasks = computed(() =>
    this.tasks().filter(task => !task.completed)
  );
  
  protected readonly completedTasks = computed(() =>
    this.tasks().filter(task => task.completed)
  );
}


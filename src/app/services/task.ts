import { computed, Service, signal } from '@angular/core';
import { Task } from '../models/task.model';

@Service()
export class TaskService {
  readonly tasks = signal<Task[]>([
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

  readonly pendingTasks = computed(() =>
    this.tasks().filter(task => !task.completed)
  );

  readonly completedTasks = computed(() =>
    this.tasks().filter(task => task.completed)
  );

  addTask(description: string): void {
    this.tasks.update(tasks => [
      ...tasks,
      {
        id: Date.now(),
        description,
        completed: false
      }
    ]);
  }

  toggleTask(id: number): void {
    this.tasks.update(tasks =>
      tasks.map(task =>
        task.id === id
          ? { ...task, completed: !task.completed }
          : task
      )
    );
  }

  deleteTask(id: number): void {
    this.tasks.update(tasks =>
      tasks.filter(task => task.id !== id)
    );
  }
}
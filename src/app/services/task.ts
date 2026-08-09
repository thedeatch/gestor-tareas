import { computed, effect, Service, signal } from '@angular/core';
import { Task } from '../models/task.model';

@Service()
export class TaskService {
  private readonly storageKey = 'gestor-tareas.tasks';

  private readonly initialTasks: Task[] = [
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
  ];

  readonly tasks = signal<Task[]>(this.loadTasks());

    constructor() {
    effect(() => {
        if (typeof localStorage === 'undefined') {
        return;
        }

        const tasks = this.tasks();
        localStorage.setItem(this.storageKey, JSON.stringify(tasks));
    });
    }

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
    updateTaskDescription(
    id: number,
    description: string
    ): void {
    this.tasks.update(tasks =>
        tasks.map(task =>
        task.id === id
            ? { ...task, description }
            : task
        )
    );
    }
  deleteTask(id: number): void {
    this.tasks.update(tasks =>
      tasks.filter(task => task.id !== id)
    );
  }

private loadTasks(): Task[] {
  if (typeof localStorage === 'undefined') {
    return this.initialTasks;
  }

  const storedTasks = localStorage.getItem(this.storageKey);

  if (!storedTasks) {
    return this.initialTasks;
  }

  try {
    return JSON.parse(storedTasks) as Task[];
  } catch {
    return this.initialTasks;
  }
}
}
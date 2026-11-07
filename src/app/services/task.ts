import {
  computed,
  inject,
  Service,
  signal
} from '@angular/core';

import { HttpClient } from '@angular/common/http';
import { Task } from '../models/task.model';
import { Observable, finalize, tap } from 'rxjs';
@Service()
export class TaskService {
  private readonly http = inject(HttpClient);

  private readonly apiUrl = '/api/tasks';

  readonly tasks = signal<Task[]>([]);
  readonly isLoading = signal(false);
  readonly loadError = signal<string | null>(null);

  readonly pendingTasks = computed(() =>
    this.tasks().filter(task => !task.completed)
  );

  readonly completedTasks = computed(() =>
    this.tasks().filter(task => task.completed)
  );
    constructor() {
      this.loadTasks();
    }

    loadTasks(): void {
    this.isLoading.set(true);
    this.loadError.set(null);

    this.http.get<Task[]>(this.apiUrl)
      .pipe(
        finalize(() => {
          this.isLoading.set(false);
        })
      )
      .subscribe({
        next: tasks => {
          this.tasks.set(tasks);
        },
        error: error => {
          console.error(
            'No fue posible cargar las tareas desde la API.',
            error
          );

          this.loadError.set(
            'No fue posible cargar las tareas. Verifica que el servidor esté funcionando.'
          );
        }
      });
  }
  addTask(description: string): Observable<Task> {
    const newTask: Omit<Task, 'id'> = {
      description,
      completed: false
    };

    return this.http.post<Task>(
      this.apiUrl,
      newTask
    ).pipe(
      tap(createdTask => {
        this.tasks.update(tasks => [
          ...tasks,
          createdTask
        ]);
      })
    );
  }

  toggleTask(id: number): Observable<Task> {
    const currentTask = this.tasks()
      .find(task => task.id === id);

    if (!currentTask) {
      throw new Error(
        `No se encontró la tarea con id ${id}`
      );
    }

    const updatedTask: Task = {
      ...currentTask,
      completed: !currentTask.completed
    };

    return this.http.put<Task>(
      `${this.apiUrl}/${id}`,
      updatedTask
    ).pipe(
      tap(savedTask => {
        this.tasks.update(tasks =>
          tasks.map(task =>
            task.id === id
              ? { ...savedTask, projectId: task.projectId, categoryId: task.categoryId }
              : task
          )
        );
      })
    );
  }
  updateTaskDescription(
    id: number,
    description: string
  ): Observable<Task> {
    const currentTask = this.tasks()
      .find(task => task.id === id);

    if (!currentTask) {
      throw new Error(
        `No se encontró la tarea con id ${id}`
      );
    }

    const updatedTask: Task = {
      ...currentTask,
      description
    };

    return this.http.put<Task>(
      `${this.apiUrl}/${id}`,
      updatedTask
    ).pipe(
      tap(savedTask => {
        this.tasks.update(tasks =>
          tasks.map(task =>
            task.id === id
              ? { ...savedTask, projectId: task.projectId, categoryId: task.categoryId }
              : task
          )
        );
      })
    );
  }
  classifyTask(id: number, projectId: number | null, categoryId: number | null): Observable<Task> {
    return this.http.put<Task>(`${this.apiUrl}/${id}/classification`, { projectId, categoryId }).pipe(
      tap(saved => this.tasks.update(tasks => tasks.map(task => task.id === id
        ? { ...task, projectId: saved.projectId, categoryId: saved.categoryId } : task)))
    );
  }

  removeClassification(id: number): void {
    this.tasks.update(tasks => tasks.map(task => ({
      ...task,
      projectId: task.projectId === id ? null : task.projectId,
      categoryId: task.categoryId === id ? null : task.categoryId
    })));
  }

  deleteTask(id: number): Observable<void> {
    return this.http.delete<void>(
      `${this.apiUrl}/${id}`
    ).pipe(
      tap(() => {
        this.tasks.update(tasks =>
          tasks.filter(task => task.id !== id)
        );
      })
    );
  }


}

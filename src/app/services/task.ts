import {
  inject,
  Service,
  signal
} from '@angular/core';

import { HttpClient } from '@angular/common/http';
import { Task, TaskDetails, TaskStatus } from '../models/task.model';
import { Observable, Subscription, finalize, tap } from 'rxjs';
import { TaskFilter } from '../models/task-filter.model';
import { taskPageParams } from '../models/task-page-params';
import { ListPage } from '../models/list-page.model';
@Service()
export class TaskService {
  private readonly http = inject(HttpClient);

  private readonly apiUrl = '/api/tasks';

  readonly tasks = signal<Task[]>([]);
  readonly isLoading = signal(false);
  readonly loadError = signal<string | null>(null);
  readonly page = signal<ListPage<Task> | null>(null);
  private request?: Subscription;
  private selection?: { filter: TaskFilter; now: Date };

  cancelLoad(): void { this.request?.unsubscribe(); }

  loadPage(filter: TaskFilter, page = 0, now = new Date()): void {
    this.request?.unsubscribe();
    this.selection = { filter: { ...filter }, now };
    this.isLoading.set(true); this.loadError.set(null);
    this.request = this.http.get<ListPage<Task>>(this.apiUrl + '/page', { params: taskPageParams(filter, page, now) })
      .pipe(finalize(() => this.isLoading.set(false))).subscribe({
        next: result => { this.tasks.set(result.items); this.page.set(result); },
        error: () => this.loadError.set('No se pudieron cargar las tareas. Vuelve a intentarlo.')
      });
  }

  private refreshPage(): void {
    if (this.selection) this.loadPage(this.selection.filter, this.page()?.page ?? 0, this.selection.now);
  }

  loadTasks(): void {
    this.request?.unsubscribe(); this.selection = undefined; this.page.set(null);
    this.isLoading.set(true);
    this.loadError.set(null);

    this.request = this.http.get<Task[]>(this.apiUrl)
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
        if (this.selection) { this.loadPage(this.selection.filter, 0, this.selection.now); return; }
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
        if (this.selection) { this.refreshPage(); return; }
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
  changeStatus(id: number, status: TaskStatus): Observable<Task> {
    return this.http.patch<Task>(`${this.apiUrl}/${id}/status`, { status }).pipe(
      tap(saved => { if (this.selection) this.refreshPage(); else this.tasks.update(tasks => tasks.map(task => task.id === id
        ? { ...task, status: saved.status, completed: saved.completed } : task)); })
    );
  }

  updateTaskDetails(id: number, details: TaskDetails): Observable<Task> {
    return this.http.put<Task>(`${this.apiUrl}/${id}/details`, details).pipe(
      tap(saved => { if (this.selection) this.refreshPage(); else this.tasks.update(tasks => tasks.map(task => task.id === id
        ? { ...saved, projectId: task.projectId, categoryId: task.categoryId } : task)); })
    );
  }

  classifyTask(id: number, projectId: number | null, categoryId: number | null): Observable<Task> {
    return this.http.put<Task>(`${this.apiUrl}/${id}/classification`, { projectId, categoryId }).pipe(
      tap(saved => { if (this.selection) this.refreshPage(); else this.tasks.update(tasks => tasks.map(task => task.id === id
        ? { ...task, projectId: saved.projectId, categoryId: saved.categoryId } : task)); })
    );
  }

  removeClassification(id: number): void {
    if (this.selection) { this.refreshPage(); return; }
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
        if (this.selection) { this.refreshPage(); return; }
        this.tasks.update(tasks =>
          tasks.filter(task => task.id !== id)
        );
      })
    );
  }


}

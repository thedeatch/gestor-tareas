import { Component, computed, inject, signal } from '@angular/core';
import { MatCardModule } from '@angular/material/card';

import { TaskFilters } from '../../components/task-filters/task-filters';
import { EMPTY_TASK_FILTER, TaskFilter, filterTasks } from '../../models/task-filter.model';
import { TaskForm } from '../../components/task-form/task-form';
import { TaskSubtasks } from '../../components/task-subtasks/task-subtasks';
import { TaskOrganization } from '../../components/task-organization/task-organization';
import { TaskItem } from '../../components/task-item/task-item';
import { TaskDetails } from '../../models/task.model';
import { getApiErrorMessage } from '../../models/api-error.model';
import { TaskService } from '../../services/task';
import { MatDialog } from '@angular/material/dialog';
import {
  ConfirmDialog,
  ConfirmDialogData
} from '../../components/confirm-dialog/confirm-dialog';
import {
  MatSnackBar,
  MatSnackBarModule
} from '@angular/material/snack-bar';
import {
  EditTaskDialog,
  EditTaskDialogData
} from '../../components/edit-task-dialog/edit-task-dialog';
import { MatButtonModule } from '@angular/material/button';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-tasks-page',
  imports: [
    MatCardModule,
    MatSnackBarModule,
    MatButtonModule,
    TaskForm,
    TaskItem,
    TaskOrganization,
    TaskSubtasks,
    TaskFilters
  ],
  templateUrl: './tasks-page.html',
  styleUrl: './tasks-page.css'
})
export class TasksPage {
  protected readonly title = signal('Gestor de tareas');
  private readonly snackBar = inject(MatSnackBar);
  private readonly taskService = inject(TaskService);
  private readonly dialog = inject(MatDialog);
  protected readonly filters = signal<TaskFilter>({ ...EMPTY_TASK_FILTER });
  protected readonly filteredTasks = computed(() => filterTasks(this.taskService.tasks(), this.filters()));
  protected readonly totalTasks = computed(() => this.taskService.tasks().length);
  protected readonly pendingTasks = computed(() => this.filteredTasks().filter(task => !task.completed));
  protected readonly completedTasks = computed(() => this.filteredTasks().filter(task => task.completed));
  protected readonly isLoading =  this.taskService.isLoading;
  protected readonly loadError =  this.taskService.loadError;
  protected readonly isCreating = signal(false);
  
  protected addTask(description: string): void {
    this.isCreating.set(true);

    this.taskService.addTask(description)
      .pipe(
        finalize(() => {
          this.isCreating.set(false);
        })
      )
      .subscribe({
        next: () => {
          this.showMessage(
            'Tarea agregada correctamente.'
          );
        },
        error: error => {
          console.error(
            'Error al agregar la tarea.',
            error
          );

          this.showMessage(
            'No fue posible agregar la tarea.'
          );
        }
      });
  }

  protected toggleTask(id: number): void {
    const task = this.taskService.tasks()
      .find(currentTask => currentTask.id === id);

    if (!task) {
      return;
    }

    const successMessage = task.completed
      ? 'Tarea movida a pendientes.'
      : 'Tarea marcada como completada.';

    this.taskService.toggleTask(id).subscribe({
      next: () => {
        this.showMessage(successMessage);
      },
      error: error => {
        console.error(
          'Error al actualizar la tarea.',
          error
        );

        this.showMessage(
          'No fue posible actualizar la tarea.'
        );
      }
    });
  }
  protected editTask(id: number): void {
    const task = this.taskService.tasks()
      .find(currentTask => currentTask.id === id);

    if (!task) {
      return;
    }

    const data: EditTaskDialogData = {
      ...task,
      status: task.status ?? (task.completed ? 'COMPLETED' : 'PENDING')
    };

    const dialogReference = this.dialog.open<EditTaskDialog, EditTaskDialogData, TaskDetails>(
      EditTaskDialog,
      {
        data,
        width: '440px'
      }
    );

    dialogReference.afterClosed().subscribe(details => {
      if (!details) return;
      this.taskService.updateTaskDetails(id, details).subscribe({
        next: () => this.showMessage('Tarea actualizada correctamente.'),
        error: error => this.showMessage(getApiErrorMessage(error, 'No fue posible editar la tarea.'))
      });
    });
  }
  protected deleteTask(id: number): void {
    const task = this.taskService.tasks()
      .find(currentTask => currentTask.id === id);

    if (!task) {
      return;
    }

    const data: ConfirmDialogData = {
      taskDescription: task.description
    };

    const dialogReference = this.dialog.open(
      ConfirmDialog,
      {
        data
      }
    );

    dialogReference.afterClosed().subscribe(confirmed => {
      if (confirmed) {
        this.taskService.deleteTask(id).subscribe({
          next: () => {
            this.showMessage(
              'Tarea eliminada correctamente.'
            );
          },
          error: error => {
            console.error(
              'Error al eliminar la tarea.',
              error
            );

            this.showMessage(
              'No fue posible eliminar la tarea.'
            );
          }
        });
      }
    });
  }
  private showMessage(message: string): void {
      this.snackBar.open(
        message,
        'Cerrar',
        {
          duration: 2500,
          horizontalPosition: 'end',
          verticalPosition: 'bottom'
        }
      );
    }
    protected retryLoad(): void {
    this.taskService.loadTasks();
  }
}
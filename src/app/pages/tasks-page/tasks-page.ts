import { Component, inject, signal } from '@angular/core';
import { MatCardModule } from '@angular/material/card';

import { TaskForm } from '../../components/task-form/task-form';
import { TaskItem } from '../../components/task-item/task-item';
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



@Component({
  selector: 'app-tasks-page',
  imports: [
    MatCardModule,
    MatSnackBarModule,
    TaskForm,
    TaskItem
  ],
  templateUrl: './tasks-page.html',
  styleUrl: './tasks-page.css'
})
export class TasksPage {
  protected readonly title = signal('Gestor de tareas');
  private readonly snackBar = inject(MatSnackBar);
  private readonly taskService = inject(TaskService);
  private readonly dialog = inject(MatDialog);
  protected readonly pendingTasks = this.taskService.pendingTasks;
  protected readonly completedTasks = this.taskService.completedTasks;

  protected addTask(description: string): void {
    this.taskService.addTask(description);
    this.showMessage('Tarea agregada correctamente.');
  }

  protected toggleTask(id: number): void {
    const task = this.taskService.tasks()
      .find(currentTask => currentTask.id === id);

    if (!task) {
      return;
    }

    this.taskService.toggleTask(id);

    const message = task.completed
      ? 'Tarea movida a pendientes.'
      : 'Tarea marcada como completada.';

    this.showMessage(message);
  }
  protected editTask(id: number): void {
    const task = this.taskService.tasks()
      .find(currentTask => currentTask.id === id);

    if (!task) {
      return;
    }

    const data: EditTaskDialogData = {
      description: task.description
    };

    const dialogReference = this.dialog.open(
      EditTaskDialog,
      {
        data,
        width: '440px'
      }
    );

    dialogReference.afterClosed().subscribe(description => {
      if (
        typeof description !== 'string' ||
        !description.trim() ||
        description === task.description
      ) {
        return;
      }

      this.taskService.updateTaskDescription(
        id,
        description.trim()
      );

      this.showMessage('Tarea actualizada correctamente.');
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
        this.taskService.deleteTask(id);
        this.showMessage('Tarea eliminada.');
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
}
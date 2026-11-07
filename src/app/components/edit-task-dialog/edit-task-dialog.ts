import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import {
  MAT_DIALOG_DATA,
  MatDialogModule
} from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { TASK_PRIORITY_LABELS, TASK_STATUS_LABELS, TaskDetails, TaskPriority, TaskStatus } from '../../models/task.model';

export interface EditTaskDialogData extends Partial<TaskDetails> {
  description: string;
}

@Component({
  selector: 'app-edit-task-dialog',
  imports: [
    FormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule
  ],
  templateUrl: './edit-task-dialog.html',
  styleUrl: './edit-task-dialog.css'
})
export class EditTaskDialog {
  protected readonly data =
    inject<EditTaskDialogData>(MAT_DIALOG_DATA);

  protected description = this.data.description;
  protected priority = this.data.priority ?? 'NORMAL';
  protected status = this.data.status ?? 'PENDING';
  protected dueDate = this.data.dueDate ?? '';
  protected tagsText = this.data.tags?.join(', ') ?? '';
  protected readonly priorities = Object.entries(TASK_PRIORITY_LABELS);
  protected readonly statuses = Object.entries(TASK_STATUS_LABELS);

  protected details(): TaskDetails {
    return {
      description: this.description.trim(),
      priority: this.priority as TaskPriority,
      status: this.status as TaskStatus,
      dueDate: this.dueDate || null,
      tags: this.tagsText.split(',').map(tag => tag.trim()).filter(Boolean)
    };
  }

  protected valid(): boolean {
    const { description, tags, dueDate } = this.details();
    return !!description && description.length <= 150 && tags.length <= 10
      && tags.every(tag => tag.length <= 30)
      && (!dueDate || /^\d{4}-\d{2}-\d{2}$/.test(dueDate));
  }
}
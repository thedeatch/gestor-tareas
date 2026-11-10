import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { provideNativeDateAdapter } from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import {
  MAT_DIALOG_DATA,
  MatDialogModule
} from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
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
    MatDatepickerModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule
  ],
  providers: [provideNativeDateAdapter()],
  templateUrl: './edit-task-dialog.html',
  styleUrl: './edit-task-dialog.css'
})
export class EditTaskDialog {
  protected readonly data =
    inject<EditTaskDialogData>(MAT_DIALOG_DATA);

  protected description = this.data.description;
  protected priority = this.data.priority ?? 'NORMAL';
  protected status = this.data.status ?? 'PENDING';
  protected dueDate = this.parseDate(this.data.dueDate);
  protected tagsText = this.data.tags?.join(', ') ?? '';
  protected readonly priorities = Object.entries(TASK_PRIORITY_LABELS);
  protected readonly statuses = Object.entries(TASK_STATUS_LABELS);

  protected details(): TaskDetails {
    return {
      description: this.description.trim(),
      priority: this.priority as TaskPriority,
      status: this.status as TaskStatus,
      dueDate: this.formatDate(this.dueDate),
      tags: this.tagsText.split(',').map(tag => tag.trim()).filter(Boolean)
    };
  }

  protected valid(): boolean {
    const { description, tags } = this.details();
    return !!description && description.length <= 150 && tags.length <= 10
      && tags.every(tag => tag.length <= 30);
  }

  private parseDate(value?: string | null): Date | null {
    if (!value) return null;
    const [year, month, day] = value.split('-').map(Number);
    if (!year || !month || !day) return null;
    return new Date(year, month - 1, day);
  }

  private formatDate(value: Date | null): string | null {
    if (!value) return null;
    const year = value.getFullYear();
    const month = String(value.getMonth() + 1).padStart(2, '0');
    const day = String(value.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}

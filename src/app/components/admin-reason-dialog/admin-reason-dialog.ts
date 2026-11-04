import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

export interface AdminReasonDialogData {
  title: string;
  message: string;
  warning?: string;
  reasonLabel: string;
  reasonPlaceholder: string;
  confirmText: string;
}

@Component({
  selector: 'app-admin-reason-dialog',
  imports: [
    FormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule
  ],
  templateUrl: './admin-reason-dialog.html',
  styleUrl: './admin-reason-dialog.css'
})
export class AdminReasonDialog {
  protected readonly data = inject<AdminReasonDialogData>(MAT_DIALOG_DATA);
  protected readonly reason = signal('');

  protected normalizedReason(): string {
    return this.reason().trim();
  }
}

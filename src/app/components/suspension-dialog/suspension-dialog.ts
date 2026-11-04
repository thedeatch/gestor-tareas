import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

export interface SuspensionDialogData {
  userName: string;
  userEmail: string;
}

@Component({
  selector: 'app-suspension-dialog',
  imports: [
    FormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule
  ],
  templateUrl: './suspension-dialog.html',
  styleUrl: './suspension-dialog.css'
})
export class SuspensionDialog {
  protected readonly data = inject<SuspensionDialogData>(MAT_DIALOG_DATA);
  protected readonly reason = signal('');

  protected normalizedReason(): string {
    return this.reason().trim();
  }
}

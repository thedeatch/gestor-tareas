import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import {
  MAT_DIALOG_DATA,
  MatDialogModule
} from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

export interface EditTaskDialogData {
  description: string;
}

@Component({
  selector: 'app-edit-task-dialog',
  imports: [
    FormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule
  ],
  templateUrl: './edit-task-dialog.html',
  styleUrl: './edit-task-dialog.css'
})
export class EditTaskDialog {
  protected readonly data =
    inject<EditTaskDialogData>(MAT_DIALOG_DATA);

  protected description = this.data.description;
}
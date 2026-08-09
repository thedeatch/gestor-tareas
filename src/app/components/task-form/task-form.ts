import { Component, output } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';

@Component({
  selector: 'app-task-form',
  imports: [
  FormsModule,
  MatButtonModule,
  MatFormFieldModule,
  MatIconModule,
  MatInputModule
],
  templateUrl: './task-form.html',
  styleUrl: './task-form.css'
})
export class TaskForm {
  protected newTaskDescription = '';

  readonly taskAdded = output<string>();

  protected addTask(): void {
    const description = this.newTaskDescription.trim();

    if (!description) {
      return;
    }

    this.taskAdded.emit(description);
    this.newTaskDescription = '';
  }
}
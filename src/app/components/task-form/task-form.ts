import { Component, output } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-task-form',
  imports: [FormsModule],
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
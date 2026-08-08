import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('Gestor de tareas');

  protected readonly tasks = signal([
    {
      id: 1,
      description: 'Aprender componentes Angular',
      completed: false
    },
    {
      id: 2,
      description: 'Practicar TypeScript',
      completed: false
    },
    {
      id: 3,
      description: 'Instalar Angular',
      completed: true
    }
  ]);

  protected toggleTask(id: number): void {
    this.tasks.update(tasks =>
      tasks.map(task =>
        task.id === id
          ? { ...task, completed: !task.completed }
          : task
      )
    );
  }

}


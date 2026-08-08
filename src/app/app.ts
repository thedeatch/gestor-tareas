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
    'Aprender componentes Angular',
    'Practicar TypeScript',
    'Instalar Angular'
  ]);

}


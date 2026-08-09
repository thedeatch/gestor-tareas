import { Routes } from '@angular/router';

import { AboutPage } from './pages/about-page/about-page';
import { TasksPage } from './pages/tasks-page/tasks-page';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'tareas',
    pathMatch: 'full'
  },
  {
    path: 'tareas',
    component: TasksPage
  },
  {
    path: 'acerca-de',
    component: AboutPage
  },
  {
    path: '**',
    redirectTo: 'tareas'
  }
];
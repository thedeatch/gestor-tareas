import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'tareas',
    pathMatch: 'full'
  },
  {
    path: 'tareas',
    loadComponent: () =>
      import('./pages/tasks-page/tasks-page')
        .then(module => module.TasksPage)
  },
  {
    path: 'acerca-de',
    loadComponent: () =>
      import('./pages/about-page/about-page')
        .then(module => module.AboutPage)
  },
  {
    path: 'login',
    loadComponent: () =>
        import('./pages/login-page/login-page')
        .then(module => module.LoginPage)
    },
  {
    path: '**',
    redirectTo: 'tareas'
  }
];
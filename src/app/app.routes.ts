import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';
import { roleGuard } from './guards/role.guard';
export const routes: Routes = [
  {
    path: '',
    redirectTo: 'tareas',
    pathMatch: 'full'
  },
  {
    path: 'tareas',
    canActivate: [authGuard],
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
    path: 'perfil',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/profile-page/profile-page')
        .then(module => module.ProfilePage)
  },
  {
    path: 'administracion/usuarios',
    canActivate: [roleGuard(['ADMIN', 'MODERATOR'])],
    loadComponent: () =>
      import('./pages/admin-users-page/admin-users-page')
        .then(module => module.AdminUsersPage)
  },
  {
    path: 'administracion/auditoria',
    canActivate: [roleGuard(['ADMIN', 'MODERATOR'])],
    loadComponent: () =>
      import('./pages/admin-audit-page/admin-audit-page')
        .then(module => module.AdminAuditPage)
  },
  {
    path: 'login',
    loadComponent: () =>
        import('./pages/login-page/login-page')
        .then(module => module.LoginPage)
  },
  {
    path: 'registro',
    loadComponent: () =>
      import('./pages/register-page/register-page')
        .then(module => module.RegisterPage)
  },
  {
    path: 'olvide-contrasena',
    loadComponent: () =>
      import('./pages/forgot-password-page/forgot-password-page')
        .then(module => module.ForgotPasswordPage)
  },
  {
    path: 'restablecer-contrasena',
    loadComponent: () =>
      import('./pages/reset-password-page/reset-password-page')
        .then(module => module.ResetPasswordPage)
  },
  {
    path: '**',
    redirectTo: 'tareas'
  }
];

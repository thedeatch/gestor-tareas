import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';
import { roleGuard } from './guards/role.guard';
import { moduleGuard } from './guards/module.guard';
export const routes: Routes = [
  {
    path: '',
    redirectTo: 'inicio',
    pathMatch: 'full'
  },
  {
    path: 'inicio',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/dashboard-page/dashboard-page')
        .then(module => module.DashboardPage)
  },
  {
    path: 'tareas',
    canActivate: [authGuard, moduleGuard('tasks')],
    loadComponent: () =>
      import('./pages/tasks-page/tasks-page')
        .then(module => module.TasksPage)
  },
  {
    path: 'grupos',
    canActivate: [authGuard, moduleGuard('communities')],
    loadComponent: () =>
      import('./pages/communities-page/communities-page')
        .then(module => module.CommunitiesPage)
  },
  {
    path: 'grupos/:id',
    canActivate: [authGuard, moduleGuard('communities')],
    loadComponent: () =>
      import('./pages/community-detail-page/community-detail-page')
        .then(module => module.CommunityDetailPage)
  },
  {
    path: 'comunidades',
    redirectTo: 'grupos',
    pathMatch: 'full'
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
    path: 'usuarios/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/public-profile-page/public-profile-page')
        .then(module => module.PublicProfilePage)
  },
  {
    path: 'mensajes',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/private-chat-page/private-chat-page')
        .then(module => module.PrivateChatPage)
  },
  {
    path: 'notificaciones',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/notifications-page/notifications-page')
        .then(module => module.NotificationsPage)
  },
  {
    path: 'administracion/usuarios',
    canActivate: [
      roleGuard(['ADMIN', 'MODERATOR']),
      moduleGuard('users')
    ],
    loadComponent: () =>
      import('./pages/admin-users-page/admin-users-page')
        .then(module => module.AdminUsersPage)
  },
  {
    path: 'administracion/auditoria',
    canActivate: [
      roleGuard(['ADMIN', 'MODERATOR']),
      moduleGuard('audit')
    ],
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
    path: 'verificar-correo',
    loadComponent: () =>
      import('./pages/verify-email-page/verify-email-page')
        .then(module => module.VerifyEmailPage)
  },
  {
    path: '**',
    redirectTo: 'inicio'
  }
];

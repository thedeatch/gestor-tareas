import { UserRole } from './auth.model';

export interface AppModule {
  id: ModuleId;
  label: string;
  title: string;
  description: string;
  icon: string;
  route: string;
  roles?: UserRole[];
}

export type ModuleId = 'tasks' | 'communities' | 'profile' | 'users' | 'audit';

export type ModuleAvailability = Record<ModuleId, boolean>;

export const APP_MODULES: readonly AppModule[] = [
  {
    id: 'tasks',
    label: 'Tareas',
    title: 'Gestión de tareas',
    description: 'Crea, organiza y completa tus actividades pendientes.',
    icon: 'task_alt',
    route: '/tareas'
  },
  {
    id: 'communities',
    label: 'Grupos',
    title: 'Grupos de la comunidad',
    description: 'Crea grupos, reúne miembros y colabora dentro de la comunidad.',
    icon: 'groups',
    route: '/grupos'
  },
  {
    id: 'profile',
    label: 'Mi perfil',
    title: 'Perfil y seguridad',
    description: 'Administra tus datos, preferencias, archivos y sesiones.',
    icon: 'manage_accounts',
    route: '/perfil'
  },
  {
    id: 'users',
    label: 'Administración',
    title: 'Administración de usuarios',
    description: 'Consulta cuentas, roles, permisos y estados de acceso.',
    icon: 'admin_panel_settings',
    route: '/administracion/usuarios',
    roles: ['ADMIN', 'MODERATOR']
  },
  {
    id: 'audit',
    label: 'Auditoría',
    title: 'Auditoría administrativa',
    description: 'Revisa el historial de cambios realizados sobre cuentas.',
    icon: 'history',
    route: '/administracion/auditoria',
    roles: ['ADMIN', 'MODERATOR']
  }
];

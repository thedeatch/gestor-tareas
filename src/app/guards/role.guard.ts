import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { UserRole } from '../models/auth.model';
import { AuthService } from '../services/auth';

export const roleGuard = (allowedRoles: UserRole[]): CanActivateFn => () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isAuthenticated()) {
    return router.createUrlTree(['/login']);
  }

  if (authService.hasAnyRole(...allowedRoles)) {
    return true;
  }

  return router.createUrlTree(['/tareas']);
};

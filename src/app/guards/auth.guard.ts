import { inject } from '@angular/core';
import {
  CanActivateFn,
  Router
} from '@angular/router';
import { catchError, map, of } from 'rxjs';

import { AuthService } from '../services/auth';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const token = authService.getToken();

  if (
    authService.isAuthenticated() &&
    token &&
    !authService.isTokenExpired(token)
  ) {
    return true;
  }

  return authService.refreshSession().pipe(
    map(() => true),
    catchError(() => {
      const hadExpiredToken = token !== null;
      authService.expireSession();
      return of(router.createUrlTree(['/login'], {
        queryParams: hadExpiredToken
          ? { session: 'expired' }
          : undefined
      }));
    })
  );
};

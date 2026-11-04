import {
  HttpErrorResponse,
  HttpInterceptorFn
} from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { AuthService } from '../services/auth';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const token = authService.getToken();
  const isApiRequest = request.url.startsWith('/api/');
  const isPublicAuthRequest = request.url.startsWith('/api/auth/');

  if (!token || !isApiRequest || isPublicAuthRequest) {
    return next(request);
  }

  const redirectToExpiredSession = (): void => {
    if (authService.expireSession()) {
      void router.navigate(['/login'], {
        queryParams: { session: 'expired' },
        replaceUrl: true
      });
    }
  };

  if (authService.isTokenExpired(token)) {
    redirectToExpiredSession();
    return throwError(() => new HttpErrorResponse({
      status: 401,
      statusText: 'Sesión expirada',
      url: request.url
    }));
  }

  return next(request.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`
    }
  })).pipe(
    catchError(error => {
      if (error instanceof HttpErrorResponse && error.status === 401) {
        redirectToExpiredSession();
      }
      return throwError(() => error);
    })
  );
};

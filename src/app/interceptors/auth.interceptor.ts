import {
  HttpErrorResponse,
  HttpHandlerFn,
  HttpInterceptorFn
} from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import {
  Observable,
  catchError,
  finalize,
  shareReplay,
  switchMap,
  throwError
} from 'rxjs';

import { AuthService } from '../services/auth';

let refreshRequest$: Observable<void> | null = null;

const refreshOnce = (authService: AuthService): Observable<void> => {
  if (!refreshRequest$) {
    refreshRequest$ = authService.refreshSession().pipe(
      finalize(() => refreshRequest$ = null),
      shareReplay({ bufferSize: 1, refCount: false })
    );
  }
  return refreshRequest$;
};

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

  const retryAfterRefresh = (
    failedRequest: typeof request,
    handler: HttpHandlerFn
  ) => refreshOnce(authService).pipe(
    switchMap(() => {
      const refreshedToken = authService.getToken();
      if (!refreshedToken) {
        return throwError(() => new HttpErrorResponse({ status: 401 }));
      }
      return handler(failedRequest.clone({
        setHeaders: { Authorization: `Bearer ${refreshedToken}` }
      }));
    }),
    catchError(error => {
      redirectToExpiredSession();
      return throwError(() => error);
    })
  );

  if (authService.isTokenExpired(token)) {
    return retryAfterRefresh(request, next);
  }

  return next(request.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`
    }
  })).pipe(
    catchError(error => {
      if (error instanceof HttpErrorResponse && error.status === 401) {
        return retryAfterRefresh(request, next);
      }
      return throwError(() => error);
    })
  );
};

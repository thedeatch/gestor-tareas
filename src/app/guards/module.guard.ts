import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';

import { ModuleId } from '../models/app-module.model';
import { ModuleConfigurationService } from '../services/module-configuration';

export const moduleGuard = (moduleId: ModuleId): CanActivateFn => () => {
  const modules = inject(ModuleConfigurationService);
  const router = inject(Router);

  return modules.load().pipe(
    map(availability => availability[moduleId]
      ? true
      : router.createUrlTree(['/inicio'], { queryParams: { disabled: moduleId } })
    ),
    catchError(() => of(router.createUrlTree(['/inicio'])))
  );
};

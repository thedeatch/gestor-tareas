import { HttpErrorResponse } from '@angular/common/http';

import { getApiErrorMessage } from './api-error.model';

describe('api error helpers', () => {
  it('returns the message from the uniform API error contract', () => {
    const error = new HttpErrorResponse({
      status: 409,
      error: {
        timestamp: '2026-11-04T00:00:00Z',
        status: 409,
        code: 'CONFLICT',
        message: 'El correo ya está registrado.',
        path: '/api/auth/register',
        fieldErrors: {}
      }
    });

    expect(getApiErrorMessage(error, 'Mensaje alternativo'))
      .toBe('El correo ya está registrado.');
  });

  it('returns the fallback when the response has no API message', () => {
    const error = new HttpErrorResponse({ status: 0 });

    expect(getApiErrorMessage(error, 'Sin conexión.'))
      .toBe('Sin conexión.');
  });
});

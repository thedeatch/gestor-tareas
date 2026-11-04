import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting
} from '@angular/common/http/testing';

import { AuthService } from './auth';

describe('AuthService', () => {
  let service: AuthService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(AuthService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
    sessionStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should store the JWT after login', () => {
    service.login({
      email: 'usuario@ejemplo.cl',
      password: 'secreto123'
    }).subscribe();

    const request = httpTesting.expectOne(
      '/api/auth/login'
    );
    expect(request.request.method).toBe('POST');

    request.flush({
      token: 'jwt-de-prueba',
      email: 'usuario@ejemplo.cl',
      name: 'Usuario de prueba'
    });

    expect(service.isAuthenticated()).toBe(true);
    expect(service.userEmail()).toBe('usuario@ejemplo.cl');
    expect(service.userName()).toBe('Usuario de prueba');
    expect(service.getToken()).toBe('jwt-de-prueba');
  });

  it('should register without starting a session before verification', () => {
    service.register({
      name: 'Nueva usuaria',
      email: 'nueva@ejemplo.cl',
      password: 'secreto123'
    }).subscribe();

    const request = httpTesting.expectOne('/api/auth/register');

    expect(request.request.method).toBe('POST');
    expect(request.request.body.name).toBe('Nueva usuaria');

    request.flush({ verificationRequired: true });

    expect(service.isAuthenticated()).toBe(false);
    expect(service.userName()).toBeNull();
  });

  it('should request a new verification email', () => {
    service.resendVerification('pendiente@ejemplo.cl').subscribe();

    const request = httpTesting.expectOne(
      '/api/auth/resend-verification'
    );
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      email: 'pendiente@ejemplo.cl'
    });
    request.flush(null);
  });
});

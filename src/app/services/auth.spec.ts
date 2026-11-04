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
      name: 'Usuario de prueba',
      role: 'USER'
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

  it('should store a renewed session', () => {
    service.refreshSession().subscribe();

    const request = httpTesting.expectOne('/api/auth/refresh');
    expect(request.request.method).toBe('POST');
    request.flush({
      token: 'jwt-renovado',
      email: 'usuario@ejemplo.cl',
      name: 'Usuario renovado',
      role: 'USER'
    });

    expect(service.getToken()).toBe('jwt-renovado');
    expect(service.isAuthenticated()).toBe(true);
  });

  it('should detect an expired JWT', () => {
    const payload = btoa(JSON.stringify({
      exp: Math.floor(Date.now() / 1000) - 60
    }));

    expect(service.isTokenExpired(`header.${payload}.signature`)).toBe(true);
  });

  it('should clear the stored session when it expires', () => {
    sessionStorage.setItem('gestor-tareas.token', 'jwt-de-prueba');
    sessionStorage.setItem('gestor-tareas.email', 'usuario@ejemplo.cl');

    expect(service.expireSession()).toBe(true);
    expect(service.getToken()).toBeNull();
    expect(service.isAuthenticated()).toBe(false);
  });
});

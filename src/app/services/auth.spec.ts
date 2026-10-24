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
      email: 'usuario@ejemplo.cl'
    });

    expect(service.isAuthenticated()).toBe(true);
    expect(service.userEmail()).toBe('usuario@ejemplo.cl');
    expect(service.getToken()).toBe('jwt-de-prueba');
  });
});

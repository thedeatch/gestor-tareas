import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { AuthService } from '../services/auth';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor session renewal', () => {
  let http: HttpTestingController;
  let client: HttpClient;
  let auth: AuthService;
  const renewed = { token: 'renewed-token', email: 'me@example.cl', name: 'Usuario', role: 'USER' };
  beforeEach(() => {
    sessionStorage.clear();
    sessionStorage.setItem('gestor-tareas.token', 'old-token');
    sessionStorage.setItem('gestor-tareas.email', 'me@example.cl');
    TestBed.configureTestingModule({ providers: [
      provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting(), provideRouter([])
    ] });
    http = TestBed.inject(HttpTestingController);
    client = TestBed.inject(HttpClient);
    auth = TestBed.inject(AuthService);
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  });
  afterEach(() => { http.verify(); sessionStorage.clear(); });

  for (const code of [403, 500]) {
    it(`keeps a renewed session when the retried resource returns ${code}`, () => {
      const error = vi.fn();
      client.get('/api/tasks').subscribe({ error });
      http.expectOne('/api/tasks').flush({}, { status: 401, statusText: 'Unauthorized' });
      http.expectOne('/api/auth/refresh').flush(renewed);
      const retry = http.expectOne('/api/tasks');
      expect(retry.request.headers.get('Authorization')).toBe('Bearer renewed-token');
      retry.flush({}, { status: code, statusText: 'Failed' });
      expect(auth.isAuthenticated()).toBe(true);
      expect(auth.getToken()).toBe(renewed.token);
      expect(error.mock.calls[0][0].status).toBe(code);
    });
  }
  it('keeps the existing session on a temporary refresh server error', () => {
    client.get('/api/tasks').subscribe({ error: () => undefined });
    http.expectOne('/api/tasks').flush({}, { status: 401, statusText: 'Unauthorized' });
    http.expectOne('/api/auth/refresh').flush({}, { status: 503, statusText: 'Unavailable' });
    expect(auth.getToken()).toBe('old-token');
  });
  it('expires the session when its refresh token is rejected', () => {
    client.get('/api/tasks').subscribe({ error: () => undefined });
    http.expectOne('/api/tasks').flush({}, { status: 401, statusText: 'Unauthorized' });
    http.expectOne('/api/auth/refresh').flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(auth.getToken()).toBeNull();
    expect(TestBed.inject(Router).navigate).toHaveBeenCalled();
  });
  it('shares renewal between simultaneous failed requests', () => {
    client.get('/api/tasks').subscribe();
    client.get('/api/notifications').subscribe();
    http.expectOne('/api/tasks').flush({}, { status: 401, statusText: 'Unauthorized' });
    http.expectOne('/api/notifications').flush({}, { status: 401, statusText: 'Unauthorized' });
    http.expectOne('/api/auth/refresh').flush(renewed);
    http.expectOne('/api/tasks').flush([]);
    http.expectOne('/api/notifications').flush([]);
  });
});

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { DashboardPage } from './dashboard-page';

describe('DashboardPage', () => {
  let fixture: ComponentFixture<DashboardPage>;

  beforeEach(async () => {
    sessionStorage.setItem('gestor-tareas.token', 'token');
    sessionStorage.setItem('gestor-tareas.email', 'user@example.com');
    sessionStorage.setItem('gestor-tareas.name', 'Usuario');
    sessionStorage.setItem('gestor-tareas.role', 'USER');

    await TestBed.configureTestingModule({
      imports: [DashboardPage],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardPage);
    await fixture.whenStable();
  });

  afterEach(() => sessionStorage.clear());

  it('should display the modules available to a regular user', () => {
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(text).toContain('Gestión de tareas');
    expect(text).toContain('Perfil y seguridad');
    expect(text).not.toContain('Administración de usuarios');
  });
});

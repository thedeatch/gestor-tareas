import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TaskItem } from './task-item';

describe('TaskItem', () => {
  let component: TaskItem;
  let fixture: ComponentFixture<TaskItem>;

  beforeEach(async () => {
  await TestBed.configureTestingModule({
    imports: [TaskItem],
  }).compileComponents();

  fixture = TestBed.createComponent(TaskItem);

  fixture.componentRef.setInput('task', {
    id: 1,
    description: 'Tarea de prueba',
    completed: false
  });

  component = fixture.componentInstance;
  await fixture.whenStable();
});

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('muestra prioridad, estado y alerta visual de vencimiento', () => {
    fixture.componentRef.setInput('task', {
      id: 2,
      description: 'Resolver incidencia',
      completed: false,
      status: 'IN_PROGRESS',
      priority: 'HIGH',
      dueDate: '2000-01-01',
      tags: ['Urgente']
    });
    fixture.detectChanges();

    const host = fixture.nativeElement as HTMLElement;
    expect(host.textContent).toContain('En progreso');
    expect(host.textContent).toContain('Alta');
    expect(host.textContent).toContain('Atrasada');
    expect(host.querySelector('.overdue')).not.toBeNull();
    expect(host.classList.contains('priority-high')).toBe(true);
  });
});

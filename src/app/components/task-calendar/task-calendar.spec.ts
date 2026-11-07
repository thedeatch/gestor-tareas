import { provideHttpClient } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { TaskCardData, TaskStatusChange } from '../../models/task.model';
import { TaskCalendar } from './task-calendar';

describe('TaskCalendar', () => {
  let fixture: ComponentFixture<TaskCalendar>;

  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date(2028, 1, 15, 12));
    TestBed.configureTestingModule({ imports: [TaskCalendar], providers: [provideHttpClient()] });
    fixture = TestBed.createComponent(TaskCalendar);
    fixture.componentRef.setInput('taskEndpoint', '/api/tasks');
  });

  afterEach(() => {
    fixture.destroy();
    vi.useRealTimers();
  });

  function setTasks(tasks: TaskCardData[]): void {
    fixture.componentRef.setInput('tasks', tasks);
    fixture.detectChanges();
  }

  function clickDate(date: string): void {
    fixture.nativeElement.querySelector(`button[data-date="${date}"]`).click();
    fixture.detectChanges();
  }

  function clickButton(label: string): void {
    const buttons = [...fixture.nativeElement.querySelectorAll('button')] as HTMLButtonElement[];
    buttons.find(button => button.textContent.trim() === label)!.click();
    fixture.detectChanges();
  }

  it('places deadlines on their civil date and keeps undated tasks accessible', () => {
    setTasks([
      { id: 1, description: 'Entrega bisiesta', dueDate: '2028-02-29' },
      { id: 2, description: 'Sin fecha', dueDate: null }
    ]);
    const cells = fixture.nativeElement.querySelectorAll('.day') as NodeListOf<HTMLButtonElement>;
    expect(cells.length).toBe(42);
    expect(cells[0].dataset['date']).toBe('2028-01-31');
    expect(fixture.nativeElement.querySelector('[data-date="2028-02-29"]').textContent).toContain('1 tarea');
    expect(fixture.nativeElement.querySelector('.undated').textContent).toContain('Sin fecha');

    clickDate('2028-02-29');
    expect(fixture.nativeElement.querySelector('section[aria-label="Tareas del día seleccionado"]')
      .textContent).toContain('Entrega bisiesta');
    clickButton('Mes siguiente');
    expect(fixture.nativeElement.querySelector('[data-date="2028-03-01"]').getAttribute('aria-pressed')).toBe('true');
    clickButton('Hoy');
    expect(fixture.nativeElement.querySelector('[data-date="2028-02-15"]').getAttribute('aria-current')).toBe('date');
  });

  it('crosses the year boundary and reflects input changes after edits or filtering', () => {
    setTasks([{ id: 3, description: 'Entrega', dueDate: '2028-01-01' }]);
    clickButton('Mes anterior');
    clickButton('Mes anterior');
    expect(fixture.nativeElement.querySelector('[data-date="2027-12-01"]').getAttribute('aria-pressed')).toBe('true');
    clickDate('2028-01-01');
    expect(fixture.nativeElement.querySelector('section').textContent).toContain('Entrega');
    setTasks([]);
    expect(fixture.nativeElement.querySelector('section').textContent).not.toContain('Entrega');
    expect(fixture.nativeElement.querySelector('section').textContent).toContain('No hay tareas');
  });

  it('emits only permitted status changes and does not mutate the task date', () => {
    const task: TaskCardData = { id: 4, description: 'Entrega', dueDate: '2028-02-15', status: 'PENDING' };
    setTasks([task]);
    const changes: TaskStatusChange[] = [];
    fixture.componentInstance.moved.subscribe(change => changes.push(change));
    const select = fixture.nativeElement.querySelector('.task-card select') as HTMLSelectElement;
    select.value = 'COMPLETED';
    select.dispatchEvent(new Event('change'));
    expect(changes).toEqual([{ id: 4, status: 'COMPLETED' }]);
    expect(task.dueDate).toBe('2028-02-15');
    expect(task.status).toBe('PENDING');

    setTasks([{ ...task, canChangeStatus: false }]);
    expect(fixture.nativeElement.querySelector('.task-card select')).toBeNull();
  });
});

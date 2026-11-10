import {
  ComponentFixture,
  TestBed
} from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting
} from '@angular/common/http/testing';

import { TaskService } from '../../services/task';
import { TaskKanban } from '../../components/task-kanban/task-kanban';
import { TasksPage } from './tasks-page';

describe('TasksPage', () => {
  let component: TasksPage;
  let fixture: ComponentFixture<TasksPage>;
  let httpTesting: HttpTestingController;

  const apiUrl =
    '/api/tasks';

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TasksPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    }).compileComponents();

    httpTesting = TestBed.inject(
      HttpTestingController
    );

    fixture = TestBed.createComponent(TasksPage);
    component = fixture.componentInstance;

    const request = httpTesting.expectOne(apiUrl);
    request.flush([]);

    fixture.detectChanges();
    await fixture.whenStable();
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('filters the displayed tasks and restores them when filters are cleared', async () => {
    const taskService = TestBed.inject(TaskService);
    taskService.tasks.set([
      { id: 1, description: 'Preparar documentación', completed: false, tags: ['Backend'] },
      { id: 2, description: 'Revisar diseño', completed: false }
    ]);
    fixture.detectChanges();
    await fixture.whenStable();

    const search = fixture.nativeElement.querySelector('input[type="search"]') as HTMLInputElement;
    search.value = 'documentacion';
    search.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelectorAll('app-task-item').length).toBe(1);
    expect(fixture.nativeElement.querySelector('app-task-item').textContent).toContain('Preparar documentación');

    const clear = [...fixture.nativeElement.querySelectorAll('button')]
      .find((button: any) => button.textContent.includes('Limpiar filtros')) as HTMLButtonElement;
    clear.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelectorAll('app-task-item').length).toBe(2);
  });
  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('saves a Kanban move as a status change and preserves the task details', async () => {
    const task = {
      id: 7, description: 'Preparar entrega', completed: false, status: 'PENDING' as const,
      priority: 'HIGH' as const, dueDate: '2027-01-02', tags: ['Backend']
    };
    TestBed.inject(TaskService).tasks.set([task]);
    fixture.detectChanges();
    await fixture.whenStable();
    const buttons = [...fixture.nativeElement.querySelectorAll('button')] as HTMLButtonElement[];
    buttons.find(button => button.textContent.trim() === 'Kanban')!.click();
    fixture.detectChanges();
    await fixture.whenStable();
    const kanban = fixture.debugElement.query(By.directive(TaskKanban))
      .componentInstance as TaskKanban;
    kanban.moved.emit({ id: task.id, status: 'IN_PROGRESS' });
    fixture.detectChanges();

    const request = httpTesting.expectOne('/api/tasks/7/status');
    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual({ status: 'IN_PROGRESS' });
    request.flush({ ...task, status: 'IN_PROGRESS' });
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('app-task-kanban section[aria-label="En progreso"] article')
      .textContent).toContain('Preparar entrega');
    expect(TestBed.inject(TaskService).tasks()[0]).toEqual({ ...task, status: 'IN_PROGRESS' });
  });

  it('keeps the search filter when switching to the calendar and shows undated results', async () => {
    TestBed.inject(TaskService).tasks.set([
      { id: 8, description: 'Entrega backend', completed: false },
      { id: 9, description: 'Revisar diseño', completed: false }
    ]);
    fixture.detectChanges();
    await fixture.whenStable();
    const search = fixture.nativeElement.querySelector('input[type="search"]') as HTMLInputElement;
    search.value = 'backend';
    search.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    await fixture.whenStable();
    const buttons = [...fixture.nativeElement.querySelectorAll('button')] as HTMLButtonElement[];
    buttons.find(button => button.textContent.trim() === 'Calendario')!.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const undated = fixture.nativeElement.querySelector('app-task-calendar .undated');
    expect(undated.textContent).toContain('Entrega backend');
    expect(undated.textContent).not.toContain('Revisar diseño');
    expect(fixture.nativeElement.querySelector('input[type="search"]').value).toBe('backend');
  });

  it('calculates productivity from filtered tasks and updates it when task data changes', async () => {
    const tasks = TestBed.inject(TaskService);
    tasks.tasks.set([
      { id: 10, description: 'Backend pendiente', completed: false, status: 'PENDING' },
      { id: 11, description: 'Backend terminado', completed: true, status: 'COMPLETED' },
      { id: 12, description: 'Diseño', completed: false, status: 'PENDING' }
    ]);
    fixture.detectChanges();
    await fixture.whenStable();
    const search = fixture.nativeElement.querySelector('input[type="search"]') as HTMLInputElement;
    search.value = 'backend';
    search.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    await fixture.whenStable();
    const buttons = [...fixture.nativeElement.querySelectorAll('button')] as HTMLButtonElement[];
    buttons.find(button => button.textContent.trim() === 'Estadísticas')!.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    const metric = (label: string) => {
      const rows = [...fixture.nativeElement.querySelectorAll('app-task-productivity dl div')] as HTMLElement[];
      return rows.find(row => row.querySelector('dt')?.textContent === label)?.querySelector('dd')?.textContent.trim();
    };
    expect(metric('Total de tareas')).toBe('2');
    expect(metric('Completadas')).toBe('1');
    tasks.tasks.update(items => items.map(task => task.id === 10
      ? { ...task, completed: true, status: 'COMPLETED' } : task));
    fixture.detectChanges();
    expect(metric('Completadas')).toBe('2');
    expect(fixture.nativeElement.querySelector('app-task-productivity').textContent).toContain('100 %');
  });
});

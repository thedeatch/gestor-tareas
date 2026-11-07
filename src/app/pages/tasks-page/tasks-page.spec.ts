import {
  ComponentFixture,
  TestBed
} from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting
} from '@angular/common/http/testing';

import { TaskService } from '../../services/task';
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
    const select = fixture.nativeElement.querySelector('app-task-kanban article select') as HTMLSelectElement;
    select.value = 'IN_PROGRESS';
    select.dispatchEvent(new Event('change'));
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
});

import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { of } from 'rxjs';
import { CommunityGoals } from './community-goals';
import { CommunityGoalOverview } from '../../models/community-goal.model';

describe('CommunityGoals', () => {
  let http: HttpTestingController;
  const url = '/api/communities/2/goals';
  const data: CommunityGoalOverview = {
    canManage: true,
    tasks: [{ id: 7, description: 'Preparar evento', priority: 'NORMAL', status: 'PENDING', dueDate: null, assigneeId: 1, assigneeName: 'Ana', canChangeStatus: true }],
    goals: [{ id: 3, title: 'Encuentro anual', description: 'Organizar el encuentro', dueDate: null, createdAt: '2026-11-18T12:00:00Z', authorName: 'Ana', taskIds: [7], totalTasks: 1, completedTasks: 0, progress: 0 }]
  };
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [CommunityGoals], providers: [
      provideHttpClient(), provideHttpClientTesting(), provideRouter([]),
      { provide: MatDialog, useValue: { open: vi.fn(() => ({ afterClosed: () => of(true) })) } }
    ] }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  function create(value = data) {
    const fixture = TestBed.createComponent(CommunityGoals);
    fixture.componentRef.setInput('groupId', 2);
    fixture.detectChanges();
    http.expectOne(url).flush(value);
    fixture.detectChanges();
    return fixture;
  }
  function click(root: HTMLElement, text: string) {
    Array.from(root.querySelectorAll('button')).find(button => button.textContent?.trim() === text)!.click();
  }
  it('preserves a failed draft and saves it on retry', () => {
    const fixture = create();
    const root: HTMLElement = fixture.nativeElement;
    click(root, 'Nuevo objetivo'); fixture.detectChanges();
    const input = root.querySelector<HTMLInputElement>('[formControlName=title]')!;
    input.value = 'Nueva meta'; input.dispatchEvent(new Event('input'));
    root.querySelector<HTMLInputElement>('input[type=checkbox]')!.click();
    fixture.detectChanges(); click(root, 'Guardar objetivo');
    const failed = http.expectOne(url);
    expect(failed.request.body).toEqual({ title: 'Nueva meta', description: '', dueDate: null, taskIds: [7] });
    failed.flush({}, { status: 500, statusText: 'Error' }); fixture.detectChanges();
    expect(input.value).toBe('Nueva meta');
    expect(root.querySelector<HTMLInputElement>('input[type=checkbox]')!.checked).toBe(true);
    expect(root.querySelector('[role=alert]')).toBeTruthy();
    click(root, 'Guardar objetivo'); http.expectOne(url).flush(data); fixture.detectChanges();
    expect(root.querySelector('form')).toBeNull();
  });
  it('updates task status and refreshes progress, reporting a refresh failure accurately', () => {
    const fixture = create(); const root: HTMLElement = fixture.nativeElement;
    const select = root.querySelector<HTMLSelectElement>('article select')!;
    select.value = 'COMPLETED'; select.dispatchEvent(new Event('change'));
    const request = http.expectOne('/api/communities/2/tasks/7/status');
    expect(request.request.body).toEqual({ status: 'COMPLETED' }); request.flush(null);
    http.expectOne(url).flush({}, { status: 500, statusText: 'Error' }); fixture.detectChanges();
    expect(root.textContent).toContain('El estado se guardó');
    click(root, 'Actualizar');
    http.expectOne(url).flush({ ...data, tasks: [{ ...data.tasks[0], status: 'COMPLETED' }], goals: [{ ...data.goals[0], completedTasks: 1, progress: 100 }] });
    fixture.detectChanges(); expect(root.textContent).toContain('¡Objetivo completado!');
  });
  it('edits linked tasks and removes goals only after confirmation', () => {
    const fixture = create(); const root: HTMLElement = fixture.nativeElement;
    click(root, 'Editar'); fixture.detectChanges(); click(root, 'Guardar objetivo');
    const update = http.expectOne(url + '/3');
    expect(update.request.method).toBe('PUT'); expect(update.request.body.taskIds).toEqual([7]);
    update.flush(data); fixture.detectChanges(); click(root, 'Eliminar');
    const remove = http.expectOne(url + '/3'); expect(remove.request.method).toBe('DELETE');
    remove.flush({ ...data, goals: [] }); fixture.detectChanges();
    expect(root.textContent).toContain('Aún no hay objetivos');
  });
  it('offers members read access and only authorized task controls', () => {
    const fixture = create({ ...data, canManage: false, tasks: [{ ...data.tasks[0], canChangeStatus: false }] });
    const root: HTMLElement = fixture.nativeElement;
    expect(root.textContent).toContain('Encuentro anual');
    expect(root.textContent).not.toContain('Nuevo objetivo');
    expect(root.querySelector('article select')).toBeNull();
    expect(root.querySelector('article a')?.getAttribute('href')).toContain('seccion=productivity&tarea=7');
  });
});

import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { CommunityTasks } from './community-tasks';
import { EMPTY_TASK_FILTER } from '../../models/task-filter.model';

describe('CommunityTasks pagination', () => {
  const url = '/api/communities/2/tasks';
  const params = new BehaviorSubject(convertToParamMap({ tarea: '7' }));
  let http: HttpTestingController;
  beforeEach(async () => {
    params.next(convertToParamMap({ tarea: '7' }));
    await TestBed.configureTestingModule({ imports: [CommunityTasks], providers: [
      provideHttpClient(), provideHttpClientTesting(), { provide: ActivatedRoute, useValue: { queryParamMap: params } }
    ] }).overrideComponent(CommunityTasks, { set: { template: '', imports: [] } }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  const result = (page = 0) => ({ items: [], total: 45, allTotal: 45, page, size: 20 });

  it('locates linked tasks then preserves filters when changing pages', () => {
    const fixture = TestBed.createComponent(CommunityTasks);
    fixture.componentRef.setInput('groupId', 2); fixture.componentRef.setInput('members', []); fixture.detectChanges();
    http.expectOne(url + '/assignees').flush([{ id: 3, name: 'Miembro anterior' }]);
    const linked = http.expectOne(r => r.url === url + '/page');
    expect(linked.request.params.get('target')).toBe('7'); linked.flush(result(2));
    expect(fixture.componentInstance['assignees']()).toContainEqual({ id: 3, name: 'Miembro anterior' });
    fixture.componentInstance['filterChanged']({ ...EMPTY_TASK_FILTER, query: 'backend' }); fixture.detectChanges();
    const filtered = http.expectOne(r => r.url === url + '/page');
    expect(filtered.request.params.has('target')).toBe(false); expect(filtered.request.params.get('page')).toBe('0'); filtered.flush(result());
    fixture.componentInstance['changePage'](1);
    const next = http.expectOne(r => r.url === url + '/page');
    expect(next.request.params.get('q')).toBe('backend'); expect(next.request.params.get('page')).toBe('1'); next.flush(result(1));
  });

  it('cancels obsolete searches and loads complete tasks only for other views', () => {
    const fixture = TestBed.createComponent(CommunityTasks);
    fixture.componentRef.setInput('groupId', 2); fixture.componentRef.setInput('members', []); fixture.detectChanges();
    http.expectOne(url + '/assignees').flush([]);
    const old = http.expectOne(r => r.url === url + '/page');
    fixture.componentInstance['filterChanged']({ ...EMPTY_TASK_FILTER, query: 'nuevo' }); fixture.detectChanges();
    expect(old.cancelled).toBe(true);
    http.expectOne(r => r.url === url + '/page').flush(result());
    fixture.componentInstance['view'].set('STATS'); fixture.detectChanges();
    http.expectOne(url).flush([]);
    expect(fixture.componentInstance['page']()).toBeNull();
  });

  it('returns to the first page after creating a task', () => {
    const fixture = TestBed.createComponent(CommunityTasks);
    fixture.componentRef.setInput('groupId', 2); fixture.componentRef.setInput('members', []); fixture.detectChanges();
    http.expectOne(url + '/assignees').flush([]);
    http.expectOne(r => r.url === url + '/page').flush(result(2));
    fixture.componentInstance['description'] = 'Nueva tarea'; fixture.componentInstance['create']();
    http.expectOne({ method: 'POST', url }).flush(null);
    const refreshed = http.expectOne(r => r.url === url + '/page');
    expect(refreshed.request.params.get('page')).toBe('0'); refreshed.flush(result());
  });
});

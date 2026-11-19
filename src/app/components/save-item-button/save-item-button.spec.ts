import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { AuthService } from '../../services/auth';
import { SavedItemsService } from '../../services/saved-items';
import { SaveItemButton } from './save-item-button';

describe('SaveItemButton', () => {
  const account = signal<string | null>('first@example.cl');
  let http: HttpTestingController;

  beforeEach(async () => {
    account.set('first@example.cl');
    await TestBed.configureTestingModule({
      imports: [SaveItemButton],
      providers: [provideHttpClient(), provideHttpClientTesting(),
        { provide: AuthService, useValue: { userEmail: account } }]
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function create(kind: 'POST' | 'IMAGE' | 'EVENT' = 'IMAGE') {
    const fixture = TestBed.createComponent(SaveItemButton);
    fixture.componentRef.setInput('groupId', 2);
    fixture.componentRef.setInput('targetId', 3);
    fixture.componentRef.setInput('kind', kind);
    fixture.detectChanges();
    return fixture;
  }

  it('loads event bookmarks and saves an event without changing registration', () => {
    const fixture = create('EVENT');
    http.expectOne('/api/saved-items/state?groupId=2').flush({ postIds: [], imageIds: [], eventIds: [3] });
    fixture.detectChanges();
    const button: HTMLButtonElement = fixture.nativeElement.querySelector('button');
    expect(button.getAttribute('aria-pressed')).toBe('true');
    button.click();
    http.expectOne({ method: 'DELETE', url: '/api/saved-items/EVENT/3' }).flush(null);
    fixture.detectChanges();
    button.click();
    const request = http.expectOne({ method: 'PUT', url: '/api/saved-items/EVENT/3' });
    expect(request.request.body).toEqual({ groupId: 2 });
    request.flush(null);
    fixture.detectChanges();
    expect(button.getAttribute('aria-pressed')).toBe('true');
  });

  it('shares the group request and updates both markers after saving and removing', () => {
    const first = create();
    const second = create();
    const state = http.expectOne('/api/saved-items/state?groupId=2');
    state.flush({ postIds: [], imageIds: [] });
    first.detectChanges();
    second.detectChanges();
    (first.nativeElement.querySelector('button') as HTMLButtonElement).click();
    first.detectChanges();
    second.detectChanges();
    expect(second.nativeElement.querySelector('button').disabled).toBe(true);
    const save = http.expectOne({ method: 'PUT', url: '/api/saved-items/IMAGE/3' });
    expect(save.request.body).toEqual({ groupId: 2 });
    save.flush(null);
    first.detectChanges();
    second.detectChanges();
    expect(second.nativeElement.querySelector('button').getAttribute('aria-pressed')).toBe('true');
    (second.nativeElement.querySelector('button') as HTMLButtonElement).click();
    http.expectOne({ method: 'DELETE', url: '/api/saved-items/IMAGE/3' }).flush(null);
    first.detectChanges();
    expect(first.nativeElement.querySelector('button').getAttribute('aria-pressed')).toBe('false');
  });

  it('keeps the previous marker after a failed save and allows retry', () => {
    const fixture = create('POST');
    http.expectOne('/api/saved-items/state?groupId=2').flush({ postIds: [], imageIds: [] });
    fixture.detectChanges();
    (fixture.nativeElement.querySelector('button') as HTMLButtonElement).click();
    http.expectOne({ method: 'PUT', url: '/api/saved-items/POST/3' })
      .flush({}, { status: 500, statusText: 'Error' });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('button').getAttribute('aria-pressed')).toBe('false');
    expect(fixture.nativeElement.querySelector('button').disabled).toBe(false);
    (fixture.nativeElement.querySelector('button') as HTMLButtonElement).click();
    http.expectOne({ method: 'PUT', url: '/api/saved-items/POST/3' }).flush(null);
  });

  it('isolates account state and ignores a delayed response from the previous account', () => {
    const fixture = create();
    const firstState = http.expectOne('/api/saved-items/state?groupId=2');
    account.set('second@example.cl');
    fixture.detectChanges();
    http.expectOne('/api/saved-items/state?groupId=2').flush({ postIds: [3], imageIds: [] });
    firstState.flush({ postIds: [], imageIds: [3] });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('button').getAttribute('aria-pressed')).toBe('false');
    const service = TestBed.inject(SavedItemsService);
    expect(service.isSaved('POST', 3)).toBe(true);
    expect(service.isSaved('IMAGE', 3)).toBe(false);
  });

  it('finishes an in-flight save when its button is destroyed', () => {
    const fixture = create();
    http.expectOne('/api/saved-items/state?groupId=2').flush({ postIds: [], imageIds: [] });
    fixture.detectChanges();
    (fixture.nativeElement.querySelector('button') as HTMLButtonElement).click();
    const request = http.expectOne({ method: 'PUT', url: '/api/saved-items/IMAGE/3' });
    fixture.destroy();
    request.flush(null);
    expect(TestBed.inject(SavedItemsService).isSaved('IMAGE', 3)).toBe(true);
  });
});

import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { CommunityPoll } from '../../models/community-poll.model';
import { CommunityPolls } from './community-polls';

describe('CommunityPolls', () => {
  const url = '/api/communities/2/polls';
  const poll: CommunityPoll = {
    id: 10, question: '¿Qué día nos reunimos?', authorId: 1, authorName: 'María',
    createdAt: '2026-11-18T10:00:00Z', closesAt: null, closed: false, canClose: true,
    selectedOptionId: null, totalVotes: 0,
    options: [{ id: 1, label: 'Viernes', votes: 0 }, { id: 2, label: 'Sábado', votes: 0 }]
  };
  let fixture: ComponentFixture<CommunityPolls>;
  let http: HttpTestingController;
  let root: HTMLElement;
  const dialog = { open: vi.fn(() => ({ afterClosed: () => of(true) })) };

  beforeEach(async () => {
    dialog.open.mockClear();
    await TestBed.configureTestingModule({
      imports: [CommunityPolls],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]),
        { provide: MatDialog, useValue: dialog }]
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(CommunityPolls);
    fixture.componentRef.setInput('groupId', 2);
    fixture.detectChanges();
    http.expectOne(url).flush([poll]);
    fixture.detectChanges();
    root = fixture.nativeElement;
  });

  afterEach(() => http.verify());

  function fill(field: HTMLInputElement | HTMLTextAreaElement, value: string): void {
    field.value = value;
    field.dispatchEvent(new Event('input'));
  }

  function openForm(): void {
    root.querySelector<HTMLButtonElement>('.poll-toolbar button')!.click();
    fixture.detectChanges();
    fill(root.querySelector<HTMLTextAreaElement>('textarea')!, '¿Qué actividad hacemos?');
    const fields = root.querySelectorAll<HTMLInputElement>('.option-fields input');
    fill(fields[0], 'Torneo');
    fill(fields[1], 'Reunión');
    fixture.detectChanges();
  }

  it('publishes from the form with a local optional deadline and displays the saved poll', () => {
    openForm();
    const localDeadline = '2099-01-01T18:30';
    fill(root.querySelector<HTMLInputElement>('input[type="datetime-local"]')!, localDeadline);
    fixture.detectChanges();
    root.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    const request = http.expectOne({ method: 'POST', url });
    expect(request.request.body).toEqual({
      question: '¿Qué actividad hacemos?', options: ['Torneo', 'Reunión'],
      closesAt: new Date(localDeadline).toISOString()
    });
    fixture.detectChanges();
    expect(root.querySelector<HTMLButtonElement>('button[type="submit"]')!.disabled).toBe(true);
    request.flush({ ...poll, id: 20, question: '¿Qué actividad hacemos?', closesAt: request.request.body.closesAt });
    fixture.detectChanges();
    expect(root.textContent).toContain('Encuesta publicada.');
    expect(root.querySelector('form')).toBeNull();
    expect(root.querySelectorAll('.poll-card')).toHaveLength(2);
  });

  it('votes and changes the selected option while keeping the total to one vote', () => {
    root.querySelector<HTMLButtonElement>('.poll-option')!.click();
    const first = http.expectOne({ method: 'PUT', url: url + '/10/vote' });
    expect(first.request.body).toEqual({ optionId: 1 });
    first.flush({ ...poll, selectedOptionId: 1, totalVotes: 1,
      options: [{ ...poll.options[0], votes: 1 }, poll.options[1]] });
    fixture.detectChanges();
    expect(root.querySelector('.poll-option')?.getAttribute('aria-pressed')).toBe('true');
    root.querySelectorAll<HTMLButtonElement>('.poll-option')[1].click();
    const second = http.expectOne({ method: 'PUT', url: url + '/10/vote' });
    expect(second.request.body).toEqual({ optionId: 2 });
    second.flush({ ...poll, selectedOptionId: 2, totalVotes: 1,
      options: [poll.options[0], { ...poll.options[1], votes: 1 }] });
    fixture.detectChanges();
    expect(root.querySelector('.poll-footer')?.textContent).toContain('1 voto en total');
    expect(root.querySelectorAll('.poll-option')[1].getAttribute('aria-pressed')).toBe('true');
    expect(root.querySelectorAll('.option-result')[1].textContent).toContain('100%');
  });

  it('preserves the form after a failed publication and rejects duplicate choices', () => {
    openForm();
    const fields = root.querySelectorAll<HTMLInputElement>('.option-fields input');
    fill(fields[1], ' TORNEO ');
    fixture.detectChanges();
    root.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    http.expectNone({ method: 'POST', url });
    fixture.detectChanges();
    expect(root.querySelector('[role="alert"]')?.textContent).toContain('diferentes');
    fill(fields[1], 'Reunión');
    fixture.detectChanges();
    root.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    http.expectOne({ method: 'POST', url }).flush({}, { status: 500, statusText: 'Error' });
    fixture.detectChanges();
    expect(root.querySelector<HTMLTextAreaElement>('textarea')!.value).toBe('¿Qué actividad hacemos?');
    expect(root.querySelector<HTMLButtonElement>('button[type="submit"]')!.disabled).toBe(false);
  });

  it('confirms closure and disables voting while retaining the results', () => {
    root.querySelector<HTMLButtonElement>('.poll-footer button')!.click();
    expect(dialog.open).toHaveBeenCalled();
    http.expectOne({ method: 'PUT', url: url + '/10/close' }).flush({ ...poll, closed: true });
    fixture.detectChanges();
    expect(root.querySelector('.poll-state')?.textContent).toBe('Cerrada');
    expect(root.querySelector<HTMLButtonElement>('.poll-option')!.disabled).toBe(true);
    expect(root.querySelector('.poll-footer button')).toBeNull();
  });

  it('prevents voting when the scheduled deadline has passed', () => {
    root.querySelectorAll<HTMLButtonElement>('.poll-toolbar button')[1].click();
    http.expectOne(url).flush([{ ...poll, closesAt: '2020-01-01T00:00:00Z' }]);
    fixture.detectChanges();
    expect(root.querySelector<HTMLButtonElement>('.poll-option')!.disabled).toBe(true);
    expect(root.querySelector('.poll-state')?.textContent).toBe('Cerrada');
  });
});

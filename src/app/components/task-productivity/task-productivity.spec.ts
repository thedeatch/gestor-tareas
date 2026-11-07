import { TestBed } from '@angular/core/testing';
import { afterEach, expect, it, vi } from 'vitest';

import { TaskProductivity } from './task-productivity';

afterEach(() => {
  TestBed.resetTestingModule();
  vi.useRealTimers();
});

it('updates overdue counts when the local date changes while the statistics remain open', () => {
  vi.useFakeTimers();
  vi.setSystemTime(new Date(2026, 10, 7, 23, 59, 45));
  TestBed.configureTestingModule({ imports: [TaskProductivity] });
  const fixture = TestBed.createComponent(TaskProductivity);
  fixture.componentRef.setInput('tasks', [
    { id: 1, description: 'Vence hoy', dueDate: '2026-11-07', status: 'PENDING' }
  ]);
  fixture.detectChanges();
  const overdueCount = () => [...fixture.nativeElement.querySelectorAll('dl div')]
    .find((metric: Element) => metric.querySelector('dt')?.textContent === 'Vencidas sin completar')
    ?.querySelector('dd')?.textContent.trim();
  expect(overdueCount()).toBe('0');
  vi.advanceTimersByTime(60_000);
  fixture.detectChanges();
  expect(overdueCount()).toBe('1');
  fixture.destroy();
});

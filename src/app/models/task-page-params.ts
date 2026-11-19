import { TaskFilter, localDate } from './task-filter.model';

export function taskPageParams(filter: TaskFilter, page: number, now: Date, target?: number): Record<string, string> {
  const params: Record<string, string> = { page: String(page), today: localDate(now) };
  for (const [key, value] of Object.entries(filter)) {
    if (value !== '') params[key === 'query' ? 'q' : key] = String(value);
  }
  if (target) params['target'] = String(target);
  return params;
}

import { TaskPriority, TaskStatus } from './task.model';

export interface TaskFilter {
  query: string;
  status: TaskStatus | '';
  priority: TaskPriority | '';
  due: '' | 'NONE' | 'OVERDUE' | 'TODAY' | 'WEEK';
  project: number | 'NONE' | '';
  category: number | 'NONE' | '';
  assignee: number | 'NONE' | '';
}

export interface TaskFilterOption {
  id: number;
  name: string;
}

export const EMPTY_TASK_FILTER: TaskFilter = {
  query: '', status: '', priority: '', due: '', project: '', category: '', assignee: ''
};

interface FilterableTask {
  description: string;
  completed?: boolean;
  status?: TaskStatus;
  priority?: TaskPriority;
  dueDate?: string | null;
  tags?: string[];
  projectId?: number | null;
  categoryId?: number | null;
  assigneeId?: number | null;
  assigneeName?: string | null;
}

const normalize = (text: string): string =>
  text.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLocaleLowerCase();

export const localDate = (date: Date): string => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};

const matchesSelection = (value: number | null | undefined, selected: number | 'NONE' | ''): boolean =>
  selected === '' || (selected === 'NONE' ? value == null : value === selected);

export function filterTasks<T extends FilterableTask>(tasks: T[], filter: TaskFilter, now = new Date()): T[] {
  const words = normalize(filter.query).trim().split(/\s+/).filter(Boolean);
  const today = localDate(now);
  const weekEnd = new Date(now);
  weekEnd.setDate(weekEnd.getDate() + 6);
  const lastDay = localDate(weekEnd);

  return tasks.filter(task => {
    const status = task.status ?? (task.completed ? 'COMPLETED' : 'PENDING');
    const text = normalize([task.description, ...(task.tags ?? []), task.assigneeName ?? ''].join(' '));
    if (!words.every(word => text.includes(word))) return false;
    if (filter.status && status !== filter.status) return false;
    if (filter.priority && (task.priority ?? 'NORMAL') !== filter.priority) return false;
    if (!matchesSelection(task.projectId, filter.project)
      || !matchesSelection(task.categoryId, filter.category)
      || !matchesSelection(task.assigneeId, filter.assignee)) return false;

    const due = task.dueDate;
    switch (filter.due) {
      case 'NONE': return !due;
      case 'OVERDUE': return !!due && due < today && status !== 'COMPLETED';
      case 'TODAY': return due === today;
      case 'WEEK': return !!due && due >= today && due <= lastDay;
      default: return true;
    }
  });
}

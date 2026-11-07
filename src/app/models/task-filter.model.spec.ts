import { describe, expect, it } from 'vitest';

import { EMPTY_TASK_FILTER, filterTasks } from './task-filter.model';
import { Task } from './task.model';

describe('Task filters', () => {
  const now = new Date(2026, 10, 7, 12);
  const tasks: Task[] = [
    { id: 1, description: 'Preparar documentación', completed: false, status: 'IN_PROGRESS',
      priority: 'HIGH', dueDate: '2026-11-06', tags: ['Backend'], projectId: 2, categoryId: 3 },
    { id: 2, description: 'Revisar entrega', completed: true, status: 'COMPLETED',
      priority: 'HIGH', dueDate: '2026-11-06', tags: ['Entrega'] },
    { id: 3, description: 'Tarea anterior', completed: false },
    { id: 4, description: 'Plan semanal', completed: false, dueDate: '2026-11-13' },
    { id: 5, description: 'Plan posterior', completed: false, dueDate: '2026-11-14' }
  ];

  it('combines accent-insensitive words, tags, status, priority and classification', () => {
    const result = filterTasks(tasks, {
      ...EMPTY_TASK_FILTER, query: 'DOCUMENTACION back', status: 'IN_PROGRESS',
      priority: 'HIGH', project: 2, category: 3, due: 'OVERDUE'
    }, now);
    expect(result.map(task => task.id)).toEqual([1]);
    expect(tasks[0].description).toBe('Preparar documentación');
  });

  it('excludes completed tasks from overdue and includes exactly seven calendar days', () => {
    expect(filterTasks(tasks, { ...EMPTY_TASK_FILTER, due: 'OVERDUE' }, now).map(task => task.id)).toEqual([1]);
    expect(filterTasks(tasks, { ...EMPTY_TASK_FILTER, due: 'WEEK' }, now).map(task => task.id)).toEqual([4]);
  });

  it('supports legacy tasks and explicit absence of classification and date', () => {
    expect(filterTasks(tasks, {
      ...EMPTY_TASK_FILTER, query: 'anterior', priority: 'NORMAL', status: 'PENDING',
      project: 'NONE', category: 'NONE', due: 'NONE'
    }, now).map(task => task.id)).toEqual([3]);
    expect(filterTasks(tasks, { ...EMPTY_TASK_FILTER }, now)).toEqual(tasks);
  });

  it('combines group assignee selection with name search', () => {
    const group = [
      { id: 1, description: 'Preparar encuentro', status: 'PENDING' as const, assigneeId: 7, assigneeName: 'María' },
      { id: 2, description: 'Preparar encuentro', status: 'PENDING' as const, assigneeId: null }
    ];
    expect(filterTasks(group, { ...EMPTY_TASK_FILTER, query: 'maria', assignee: 7 }, now)
      .map(task => task.id)).toEqual([1]);
    expect(filterTasks(group, { ...EMPTY_TASK_FILTER, assignee: 'NONE' }, now)
      .map(task => task.id)).toEqual([2]);
  });
});
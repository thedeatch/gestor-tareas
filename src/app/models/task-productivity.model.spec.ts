import { describe, expect, it } from 'vitest';

import { taskProductivity } from './task-productivity.model';
import { TaskCardData } from './task.model';

describe('Task productivity', () => {
  const now = new Date(2026, 10, 7, 12);

  it('handles empty results and tasks created before advanced details existed', () => {
    const empty = taskProductivity([], now);
    expect(empty.total).toBe(0);
    expect(empty.completionRate).toBe(0);
    expect(empty.workloads).toEqual([]);

    const tasks: TaskCardData[] = [
      { id: 1, description: 'Anterior pendiente', completed: false },
      { id: 2, description: 'Anterior completada', completed: true }
    ];
    const result = taskProductivity(tasks, now);
    expect(result.statuses).toEqual({ PENDING: 1, IN_PROGRESS: 0, COMPLETED: 1 });
    expect(result.priorities).toEqual({ LOW: 0, NORMAL: 2, HIGH: 0 });
    expect(result.completionRate).toBe(50);
    expect(result.undatedRemaining).toBe(1);
    expect(tasks[0].status).toBeUndefined();
  });

  it('excludes completed tasks from outstanding deadlines and uses seven inclusive calendar days', () => {
    const result = taskProductivity([
      { id: 1, description: 'Vencida', status: 'PENDING', dueDate: '2026-11-06', priority: 'HIGH' },
      { id: 2, description: 'Ya completada', status: 'COMPLETED', dueDate: '2026-11-06', priority: 'LOW' },
      { id: 3, description: 'Hoy', status: 'IN_PROGRESS', dueDate: '2026-11-07' },
      { id: 4, description: 'Último día', status: 'PENDING', dueDate: '2026-11-13' },
      { id: 5, description: 'Fuera de la semana', status: 'PENDING', dueDate: '2026-11-14' },
      { id: 6, description: 'Sin plazo', status: 'PENDING' }
    ], now);
    expect(result.total).toBe(6);
    expect(result.remaining).toBe(5);
    expect(result.overdue).toBe(1);
    expect(result.dueToday).toBe(1);
    expect(result.dueThisWeek).toBe(2);
    expect(result.undatedRemaining).toBe(1);
    expect(result.completionRate).toBe(17);
    expect(result.priorities).toEqual({ LOW: 1, NORMAL: 4, HIGH: 1 });
  });

  it('groups workload by user ID and includes unassigned tasks', () => {
    const result = taskProductivity([
      { id: 1, description: 'A', assigneeId: 7, assigneeName: 'Alex', status: 'PENDING', dueDate: '2026-11-01' },
      { id: 2, description: 'B', assigneeId: 7, assigneeName: 'Alex', status: 'COMPLETED' },
      { id: 3, description: 'C', assigneeId: 8, assigneeName: 'Alex', status: 'PENDING' },
      { id: 4, description: 'D', assigneeId: null, status: 'IN_PROGRESS' }
    ], now);
    expect(result.workloads).toHaveLength(3);
    expect(result.workloads.find(person => person.assigneeId === 7)).toEqual({
      assigneeId: 7, name: 'Alex', total: 2, completed: 1, remaining: 1, overdue: 1
    });
    expect(result.workloads.find(person => person.assigneeId === null)?.remaining).toBe(1);
    expect(result.workloads.reduce((sum, person) => sum + person.total, 0)).toBe(result.total);
  });
});

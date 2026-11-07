import { localDate } from './task-filter.model';
import { TaskCardData, TaskPriority, TaskStatus } from './task.model';

export interface TaskWorkload {
  assigneeId: number | null;
  name: string;
  total: number;
  completed: number;
  remaining: number;
  overdue: number;
}

export interface TaskProductivity {
  total: number;
  completed: number;
  remaining: number;
  completionRate: number;
  overdue: number;
  dueToday: number;
  dueThisWeek: number;
  undatedRemaining: number;
  statuses: Record<TaskStatus, number>;
  priorities: Record<TaskPriority, number>;
  workloads: TaskWorkload[];
}

export function taskProductivity(tasks: TaskCardData[], now = new Date()): TaskProductivity {
  const today = localDate(now);
  const weekEnd = new Date(now);
  weekEnd.setDate(weekEnd.getDate() + 6);
  const lastDay = localDate(weekEnd);
  const statuses: Record<TaskStatus, number> = { PENDING: 0, IN_PROGRESS: 0, COMPLETED: 0 };
  const priorities: Record<TaskPriority, number> = { LOW: 0, NORMAL: 0, HIGH: 0 };
  const workloads = new Map<number | null, TaskWorkload>();
  let overdue = 0;
  let dueToday = 0;
  let dueThisWeek = 0;
  let undatedRemaining = 0;

  for (const task of tasks) {
    const status = task.status ?? (task.completed ? 'COMPLETED' : 'PENDING');
    const completed = status === 'COMPLETED';
    const isOverdue = !completed && !!task.dueDate && task.dueDate < today;
    statuses[status]++;
    priorities[task.priority ?? 'NORMAL']++;
    if (isOverdue) overdue++;
    if (!completed) {
      if (!task.dueDate) undatedRemaining++;
      if (task.dueDate === today) dueToday++;
      if (task.dueDate && task.dueDate >= today && task.dueDate <= lastDay) dueThisWeek++;
    }

    const assigneeId = task.assigneeId ?? null;
    let workload = workloads.get(assigneeId);
    if (!workload) {
      workload = {
        assigneeId,
        name: assigneeId === null ? 'Sin asignar' : task.assigneeName ?? 'Miembro anterior',
        total: 0, completed: 0, remaining: 0, overdue: 0
      };
      workloads.set(assigneeId, workload);
    }
    workload.total++;
    if (completed) workload.completed++;
    else workload.remaining++;
    if (isOverdue) workload.overdue++;
  }

  return {
    total: tasks.length,
    completed: statuses.COMPLETED,
    remaining: tasks.length - statuses.COMPLETED,
    completionRate: tasks.length === 0 ? 0 : Math.round(statuses.COMPLETED / tasks.length * 100),
    overdue, dueToday, dueThisWeek, undatedRemaining, statuses, priorities,
    workloads: [...workloads.values()].sort((first, second) =>
      second.remaining - first.remaining || first.name.localeCompare(second.name, 'es'))
  };
}

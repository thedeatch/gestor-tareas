export type TaskPriority = 'LOW' | 'NORMAL' | 'HIGH';
export type TaskStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED';

export interface TaskCardData {
  id: number;
  description: string;
  completed?: boolean;
  status?: TaskStatus;
  priority?: TaskPriority;
  dueDate?: string | null;
  tags?: string[];
  assigneeId?: number | null;
  assigneeName?: string | null;
  canChangeStatus?: boolean;
}

export interface TaskStatusChange {
  id: number;
  status: TaskStatus;
}

export interface TaskDetails {
  description: string;
  priority: TaskPriority;
  status: TaskStatus;
  dueDate: string | null;
  tags: string[];
}

export const TASK_PRIORITY_LABELS: Record<TaskPriority, string> = {
  LOW: 'Baja', NORMAL: 'Normal', HIGH: 'Alta'
};

export const TASK_STATUS_LABELS: Record<TaskStatus, string> = {
  PENDING: 'Pendiente', IN_PROGRESS: 'En progreso', COMPLETED: 'Completada'
};

export interface Task extends Partial<TaskDetails> {
  id: number;
  description: string;
  completed: boolean;
  projectId?: number | null;
  categoryId?: number | null;
}

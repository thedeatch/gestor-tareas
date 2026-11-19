import { TaskPriority, TaskStatus } from './task.model';

export interface CommunityTask {
  id: number;
  description: string;
  priority: TaskPriority;
  status: TaskStatus;
  dueDate: string | null;
  assigneeId: number | null;
  assigneeName: string | null;
  canChangeStatus: boolean;
}
export interface CommunityGoalDraft {
  title: string;
  description: string;
  dueDate: string | null;
  taskIds: number[];
}
export interface CommunityGoal extends CommunityGoalDraft {
  id: number;
  createdAt: string;
  authorName: string | null;
  totalTasks: number;
  completedTasks: number;
  progress: number;
}
export interface CommunityGoalOverview {
  goals: CommunityGoal[];
  tasks: CommunityTask[];
  canManage: boolean;
}

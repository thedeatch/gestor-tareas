export interface Task {
  id: number;
  description: string;
  completed: boolean;
  projectId?: number | null;
  categoryId?: number | null;
}
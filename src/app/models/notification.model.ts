export type NotificationType =
  | 'SYSTEM'
  | 'INVITATION'
  | 'TASK'
  | 'COMMENT'
  | 'EVENT'
  | 'MODERATION';

export interface UserNotification {
  id: number;
  type: NotificationType;
  title: string;
  message: string;
  targetUrl: string | null;
  actorName: string | null;
  actorEmail: string | null;
  readAt: string | null;
  createdAt: string;
}

export interface NotificationPage {
  content: UserNotification[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

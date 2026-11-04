import { UserRole } from './auth.model';

export interface AdminUser {
  id: number;
  name: string;
  email: string;
  role: UserRole;
  active: boolean;
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export type AdminAuditAction =
  | 'ROLE_CHANGED'
  | 'ACCOUNT_SUSPENDED'
  | 'ACCOUNT_ACTIVATED';

export interface AdminAuditEntry {
  id: number;
  actorEmail: string;
  targetUserId: number;
  targetEmail: string;
  action: AdminAuditAction;
  previousValue: string;
  newValue: string;
  reason: string | null;
  createdAt: string;
}

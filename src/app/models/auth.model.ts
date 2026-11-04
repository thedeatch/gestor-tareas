export type UserRole = 'USER' | 'MODERATOR' | 'ADMIN';

export interface AuthResponse {
  token: string;
  email: string;
  name: string;
  role: UserRole;
}

export interface AuthCredentials {
  email: string;
  password: string;
}

export interface RegistrationData extends AuthCredentials {
  name: string;
}

export interface UserProfile {
  name: string;
  email: string;
  hasAvatar: boolean;
  role: UserRole;
}

export interface ProfileUpdateData {
  name: string;
  email: string;
  currentPassword: string;
}

export interface PasswordChangeData {
  currentPassword: string;
  newPassword: string;
}

export interface UserSession {
  id: number;
  device: string;
  ipAddress: string | null;
  createdAt: string;
  lastUsedAt: string;
  expiresAt: string;
  current: boolean;
}

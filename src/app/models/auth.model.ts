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
  bio: string;
  publicProfile: boolean;
  showPublicCommunities: boolean;
}

export interface ProfileUpdateData {
  name: string;
  email: string;
  currentPassword: string;
  bio: string;
  publicProfile: boolean;
  showPublicCommunities: boolean;
}

export interface PublicProfileCommunity {
  id: number;
  name: string;
  slug: string;
  hasImage: boolean;
  role: import('./community.model').CommunityRole;
}

export interface PublicUserProfile {
  id: number;
  name: string;
  bio: string;
  hasAvatar: boolean;
  joinedAt: string;
  communities: PublicProfileCommunity[];
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

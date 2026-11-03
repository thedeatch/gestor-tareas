export interface AuthResponse {
  token: string;
  email: string;
  name: string;
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

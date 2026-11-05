export type ThemePreference = 'LIGHT' | 'DARK';

export interface UserPreferences {
  theme: ThemePreference;
  reducedMotion: boolean;
}

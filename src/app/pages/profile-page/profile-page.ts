import {
  Component,
  effect,
  OnDestroy,
  OnInit,
  inject,
  signal
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';

import { AuthService } from '../../services/auth';
import { AvatarCropDialog } from '../../components/avatar-crop-dialog/avatar-crop-dialog';
import { UserSession } from '../../models/auth.model';
import { StoredFile } from '../../models/stored-file.model';
import { FileStorageService } from '../../services/file-storage';
import { ThemePreference } from '../../models/user-preferences.model';
import { UserPreferencesService } from '../../services/user-preferences';
import {
  ConfirmDialog,
  ConfirmDialogData
} from '../../components/confirm-dialog/confirm-dialog';

import { BlockedUsers } from '../../components/blocked-users/blocked-users';

@Component({
  selector: 'app-profile-page',
  imports: [
    BlockedUsers,
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatSlideToggleModule,
    DatePipe
  ],
  templateUrl: './profile-page.html',
  styleUrl: './profile-page.css'
})
export class ProfilePage implements OnInit, OnDestroy {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly fileStorage = inject(FileStorageService);
  private readonly preferencesService = inject(UserPreferencesService);

  protected readonly profile = {
    name: '',
    email: '',
    currentPassword: '',
    bio: '',
    publicProfile: true,
    showPublicCommunities: true
  };

  protected readonly password = {
    currentPassword: '',
    newPassword: '',
    confirmation: ''
  };

  protected readonly isLoading = signal(true);
  protected readonly isSavingProfile = signal(false);
  protected readonly isChangingPassword = signal(false);
  protected readonly isUploadingAvatar = signal(false);
  protected readonly hideCurrentPassword = signal(true);
  protected readonly hideNewPassword = signal(true);
  protected readonly avatarUrl = signal<string | null>(null);
  protected readonly message = signal<string | null>(null);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly sessions = signal<UserSession[]>([]);
  protected readonly isLoadingSessions = signal(true);
  protected readonly sessionInProgress = signal<number | null>(null);
  protected readonly files = signal<StoredFile[]>([]);
  protected readonly isLoadingFiles = signal(true);
  protected readonly fileInProgress = signal<number | null>(null);
  protected readonly preferences = {
    theme: 'LIGHT' as ThemePreference,
    reducedMotion: false
  };
  protected readonly isLoadingPreferences = signal(true);
  protected readonly isSavingPreferences = signal(false);

  constructor() {
    effect(() => {
      const current = this.preferencesService.preferences();
      this.preferences.theme = current.theme;
      this.preferences.reducedMotion = current.reducedMotion;
    });
  }

  ngOnInit(): void {
    this.loadProfile();
    this.loadSessions();
    this.loadFiles();
    this.loadPreferences();
  }

  ngOnDestroy(): void {
    this.revokeAvatarUrl();
  }

  protected passwordsMatch(): boolean {
    return this.password.newPassword === this.password.confirmation;
  }

  protected toggleCurrentPasswordVisibility(): void {
    this.hideCurrentPassword.update(hidden => !hidden);
  }

  protected toggleNewPasswordVisibility(): void {
    this.hideNewPassword.update(hidden => !hidden);
  }

  protected saveProfile(): void {
    this.beginAction();
    this.isSavingProfile.set(true);
    this.authService.updateProfile(this.profile).pipe(
      finalize(() => this.isSavingProfile.set(false))
    ).subscribe({
      next: () => {
        this.profile.currentPassword = '';
        this.message.set('Perfil actualizado correctamente.');
      },
      error: error => this.errorMessage.set(
        error.status === 409
          ? 'El correo ya está asociado a otra cuenta.'
          : error.status === 401
            ? 'La contraseña actual es incorrecta.'
            : 'No fue posible actualizar el perfil.'
      )
    });
  }

  protected changePassword(): void {
    if (!this.passwordsMatch()) {
      this.errorMessage.set('Las contraseñas no coinciden.');
      return;
    }

    this.beginAction();
    this.isChangingPassword.set(true);
    this.authService.changePassword({
      currentPassword: this.password.currentPassword,
      newPassword: this.password.newPassword
    }).pipe(
      finalize(() => this.isChangingPassword.set(false))
    ).subscribe({
      next: () => {
        this.authService.logoutFromServer().subscribe({
          complete: () => void this.router.navigate(['/login'], {
            state: { passwordChanged: true }
          }),
          error: () => void this.router.navigate(['/login'], {
            state: { passwordChanged: true }
          })
        });
      },
      error: error => this.errorMessage.set(
        error.status === 401
          ? 'La contraseña actual es incorrecta.'
          : error.status === 400
            ? 'La contraseña nueva debe ser diferente y tener entre 8 y 72 caracteres.'
            : 'No fue posible cambiar la contraseña.'
      )
    });
  }

  protected selectAvatar(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }

    if (file.size > 2 * 1024 * 1024) {
      this.errorMessage.set('La imagen debe pesar como máximo 2 MB.');
      input.value = '';
      return;
    }

    const sourceUrl = URL.createObjectURL(file);
    this.dialog.open(AvatarCropDialog, {
      data: { sourceUrl },
      width: 'min(92vw, 520px)',
      maxWidth: '92vw'
    }).afterClosed().subscribe((croppedFile?: File) => {
      URL.revokeObjectURL(sourceUrl);
      input.value = '';
      if (croppedFile) {
        this.uploadAvatar(croppedFile);
      }
    });
  }

  protected removeAvatar(): void {
    this.beginAction();
    this.isUploadingAvatar.set(true);
    this.authService.removeAvatar().pipe(
      finalize(() => this.isUploadingAvatar.set(false))
    ).subscribe({
      next: () => {
        this.revokeAvatarUrl();
        this.avatarUrl.set(null);
        this.message.set('Foto de perfil eliminada.');
      },
      error: () => this.errorMessage.set(
        'No fue posible eliminar la foto.'
      )
    });
  }

  protected closeSession(session: UserSession): void {
    this.beginAction();
    this.sessionInProgress.set(session.id);
    this.authService.closeSession(session.id).pipe(
      finalize(() => this.sessionInProgress.set(null))
    ).subscribe({
      next: () => {
        if (session.current) {
          this.authService.logout();
          void this.router.navigate(['/login'], {
            state: { sessionClosed: true }
          });
          return;
        }
        this.message.set('La sesión seleccionada fue cerrada.');
        this.loadSessions();
      },
      error: () => this.errorMessage.set('No fue posible cerrar la sesión.')
    });
  }

  protected closeOtherSessions(): void {
    this.beginAction();
    this.sessionInProgress.set(-1);
    this.authService.closeOtherSessions().pipe(
      finalize(() => this.sessionInProgress.set(null))
    ).subscribe({
      next: () => {
        this.message.set('Las demás sesiones fueron cerradas.');
        this.loadSessions();
      },
      error: () => this.errorMessage.set('No fue posible cerrar las demás sesiones.')
    });
  }

  protected selectFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }

    if (file.size > 10 * 1024 * 1024) {
      this.errorMessage.set('El archivo debe pesar como máximo 10 MB.');
      input.value = '';
      return;
    }

    this.beginAction();
    this.fileInProgress.set(-1);
    this.fileStorage.upload(file).pipe(
      finalize(() => {
        this.fileInProgress.set(null);
        input.value = '';
      })
    ).subscribe({
      next: () => {
        this.message.set('Archivo guardado correctamente.');
        this.loadFiles();
      },
      error: error => this.errorMessage.set(
        error.status === 400
          ? 'Formato no permitido o archivo inválido.'
          : 'No fue posible guardar el archivo.'
      )
    });
  }

  protected downloadFile(file: StoredFile): void {
    this.beginAction();
    this.fileInProgress.set(file.id);
    this.fileStorage.download(file.id).pipe(
      finalize(() => this.fileInProgress.set(null))
    ).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = file.name;
        link.click();
        URL.revokeObjectURL(url);
      },
      error: () => this.errorMessage.set('No fue posible descargar el archivo.')
    });
  }

  protected deleteFile(file: StoredFile): void {
    const data: ConfirmDialogData = {
      title: 'Eliminar archivo',
      itemLabel: 'el archivo',
      itemName: file.name,
      confirmLabel: 'Eliminar'
    };

    this.dialog.open(ConfirmDialog, { data })
      .afterClosed()
      .subscribe(confirmed => {
        if (confirmed) {
          this.performFileDeletion(file);
        }
      });
  }

  private performFileDeletion(file: StoredFile): void {
    this.beginAction();
    this.fileInProgress.set(file.id);
    this.fileStorage.delete(file.id).pipe(
      finalize(() => this.fileInProgress.set(null))
    ).subscribe({
      next: () => {
        this.message.set('Archivo eliminado.');
        this.loadFiles();
      },
      error: () => this.errorMessage.set('No fue posible eliminar el archivo.')
    });
  }

  protected formatFileSize(size: number): string {
    return size < 1024 * 1024
      ? `${Math.max(1, Math.round(size / 1024))} KB`
      : `${(size / 1024 / 1024).toFixed(1)} MB`;
  }

  protected savePreferences(): void {
    this.beginAction();
    this.isSavingPreferences.set(true);
    this.preferencesService.update(this.preferences).pipe(
      finalize(() => this.isSavingPreferences.set(false))
    ).subscribe({
      next: () => this.message.set('Preferencias guardadas correctamente.'),
      error: () => this.errorMessage.set(
        'No fue posible guardar las preferencias.'
      )
    });
  }

  private loadProfile(): void {
    this.authService.getProfile().pipe(
      finalize(() => this.isLoading.set(false))
    ).subscribe({
      next: profile => {
        this.profile.name = profile.name;
        this.profile.email = profile.email;
        this.profile.bio = profile.bio;
        this.profile.publicProfile = profile.publicProfile;
        this.profile.showPublicCommunities = profile.showPublicCommunities;
        if (profile.hasAvatar) {
          this.loadAvatar();
        }
      },
      error: () => this.errorMessage.set('No fue posible cargar el perfil.')
    });
  }

  private loadSessions(): void {
    this.isLoadingSessions.set(true);
    this.authService.getSessions().pipe(
      finalize(() => this.isLoadingSessions.set(false))
    ).subscribe({
      next: sessions => this.sessions.set(sessions),
      error: () => this.errorMessage.set('No fue posible cargar las sesiones activas.')
    });
  }

  private loadFiles(): void {
    this.isLoadingFiles.set(true);
    this.fileStorage.list().pipe(
      finalize(() => this.isLoadingFiles.set(false))
    ).subscribe({
      next: files => this.files.set(files),
      error: () => this.errorMessage.set('No fue posible cargar los archivos.')
    });
  }

  private loadPreferences(): void {
    this.isLoadingPreferences.set(true);
    this.preferencesService.load().pipe(
      finalize(() => this.isLoadingPreferences.set(false))
    ).subscribe({
      next: preferences => {
        this.preferences.theme = preferences.theme;
        this.preferences.reducedMotion = preferences.reducedMotion;
      },
      error: () => this.errorMessage.set(
        'No fue posible cargar las preferencias.'
      )
    });
  }

  private loadAvatar(): void {
    this.authService.getAvatar().subscribe({
      next: blob => {
        this.revokeAvatarUrl();
        this.avatarUrl.set(URL.createObjectURL(blob));
      }
    });
  }

  private uploadAvatar(file: File): void {
    this.beginAction();
    this.isUploadingAvatar.set(true);
    this.authService.updateAvatar(file).pipe(
      finalize(() => this.isUploadingAvatar.set(false))
    ).subscribe({
      next: () => {
        this.message.set('Foto de perfil actualizada.');
        this.loadAvatar();
      },
      error: () => this.errorMessage.set(
        'No fue posible guardar la imagen recortada.'
      )
    });
  }

  private beginAction(): void {
    this.message.set(null);
    this.errorMessage.set(null);
  }

  private revokeAvatarUrl(): void {
    const currentUrl = this.avatarUrl();
    if (currentUrl) {
      URL.revokeObjectURL(currentUrl);
    }
  }
}

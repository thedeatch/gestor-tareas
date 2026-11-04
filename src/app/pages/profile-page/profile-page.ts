import {
  Component,
  OnDestroy,
  OnInit,
  inject,
  signal
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';

import { AuthService } from '../../services/auth';
import { AvatarCropDialog } from '../../components/avatar-crop-dialog/avatar-crop-dialog';

@Component({
  selector: 'app-profile-page',
  imports: [
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './profile-page.html',
  styleUrl: './profile-page.css'
})
export class ProfilePage implements OnInit, OnDestroy {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);

  protected readonly profile = {
    name: '',
    email: '',
    currentPassword: ''
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

  ngOnInit(): void {
    this.loadProfile();
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

  private loadProfile(): void {
    this.authService.getProfile().pipe(
      finalize(() => this.isLoading.set(false))
    ).subscribe({
      next: profile => {
        this.profile.name = profile.name;
        this.profile.email = profile.email;
        if (profile.hasAvatar) {
          this.loadAvatar();
        }
      },
      error: () => this.errorMessage.set('No fue posible cargar el perfil.')
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

import { Component, OnDestroy, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';

import { PublicUserProfile } from '../../models/auth.model';
import { CommunityRole } from '../../models/community.model';
import { finalize } from 'rxjs';
import { BlockStatus, UserBlocksService } from '../../services/user-blocks';
import { getApiErrorMessage } from '../../models/api-error.model';
import { AuthService } from '../../services/auth';
import { ModuleConfigurationService } from '../../services/module-configuration';
import { CommunityAvatar } from '../../components/community-avatar/community-avatar';

@Component({
  selector: 'app-public-profile-page',
  imports: [DatePipe, RouterLink, MatButtonModule, MatCardModule, MatIconModule, CommunityAvatar],
  templateUrl: './public-profile-page.html',
  styleUrl: './public-profile-page.css'
})
export class PublicProfilePage implements OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly auth = inject(AuthService);
  private readonly blocks = inject(UserBlocksService);
  private readonly moduleConfiguration = inject(ModuleConfigurationService);
  protected readonly messagingEnabled = signal(false);
  protected readonly block = signal<BlockStatus | null>(null);
  protected readonly blockBusy = signal(false);
  protected readonly blockError = signal('');
  protected readonly profile = signal<PublicUserProfile | null>(null);
  protected readonly avatarUrl = signal<string | null>(null);
  protected readonly isLoading = signal(true);
  protected readonly error = signal<string | null>(null);

  constructor() {
    this.moduleConfiguration.load().subscribe({
      next: availability => this.messagingEnabled.set(availability.messaging),
      error: () => this.messagingEnabled.set(false)
    });
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isInteger(id) || id < 1) {
      this.error.set('El perfil solicitado no es válido.');
      this.isLoading.set(false);
      return;
    }
    this.auth.getPublicProfile(id).subscribe({
      next: profile => {
        this.profile.set(profile);
        this.isLoading.set(false);
        this.blocks.status(id).subscribe({
          next: status => this.block.set(status),
          error: () => this.blockError.set('No se pudo consultar el estado del bloqueo.')
        });
        if (profile.hasAvatar) this.loadAvatar(id);
      },
      error: () => {
        this.error.set('Este perfil no está disponible o es privado.');
        this.isLoading.set(false);
      }
    });
  }

  protected toggleBlock(id: number): void {
    const status = this.block();
    if (!status?.canBlock || this.blockBusy()) return;
    this.blockBusy.set(true);
    this.blockError.set('');
    const blocked = !status.blocked;
    this.blocks.set(id, blocked).pipe(finalize(() => this.blockBusy.set(false))).subscribe({
      next: () => this.block.set({ ...status, blocked }),
      error: error => this.blockError.set(getApiErrorMessage(error, 'No se pudo actualizar el bloqueo.'))
    });
  }

  protected roleLabel(role: CommunityRole): string {
    return ({ OWNER: 'Propietario', COMMUNITY_ADMIN: 'Administrador',
      COMMUNITY_MODERATOR: 'Moderador', MEMBER: 'Miembro' })[role];
  }

  ngOnDestroy(): void {
    const url = this.avatarUrl();
    if (url) URL.revokeObjectURL(url);
  }

  private loadAvatar(id: number): void {
    this.auth.getPublicAvatar(id).subscribe(blob =>
      this.avatarUrl.set(URL.createObjectURL(blob))
    );
  }
}

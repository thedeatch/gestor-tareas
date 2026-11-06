import { Component, OnDestroy, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';

import { PublicUserProfile } from '../../models/auth.model';
import { CommunityRole } from '../../models/community.model';
import { AuthService } from '../../services/auth';

@Component({
  selector: 'app-public-profile-page',
  imports: [DatePipe, RouterLink, MatButtonModule, MatCardModule, MatIconModule],
  templateUrl: './public-profile-page.html',
  styleUrl: './public-profile-page.css'
})
export class PublicProfilePage implements OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly auth = inject(AuthService);
  protected readonly profile = signal<PublicUserProfile | null>(null);
  protected readonly avatarUrl = signal<string | null>(null);
  protected readonly isLoading = signal(true);
  protected readonly error = signal<string | null>(null);

  constructor() {
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
        if (profile.hasAvatar) this.loadAvatar(id);
      },
      error: () => {
        this.error.set('Este perfil no está disponible o es privado.');
        this.isLoading.set(false);
      }
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

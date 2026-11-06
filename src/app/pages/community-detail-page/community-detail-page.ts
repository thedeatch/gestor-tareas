import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';

import { CommunityDetail, CommunityRole, CommunityVisibility } from '../../models/community.model';
import { CommunityService } from '../../services/community';

@Component({
  selector: 'app-community-detail-page',
  imports: [RouterLink, MatButtonModule, MatCardModule, MatIconModule],
  templateUrl: './community-detail-page.html',
  styleUrl: './community-detail-page.css'
})
export class CommunityDetailPage {
  private readonly route = inject(ActivatedRoute);
  private readonly service = inject(CommunityService);
  protected readonly community = signal<CommunityDetail | null>(null);
  protected readonly isLoading = signal(true);
  protected readonly error = signal<string | null>(null);

  constructor() { this.load(); }

  protected load(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isInteger(id) || id < 1) {
      this.error.set('La comunidad solicitada no es válida.');
      this.isLoading.set(false);
      return;
    }
    this.isLoading.set(true);
    this.service.findById(id).subscribe({
      next: community => { this.community.set(community); this.isLoading.set(false); },
      error: () => { this.error.set('No fue posible abrir la comunidad.'); this.isLoading.set(false); }
    });
  }

  protected visibilityLabel(value: CommunityVisibility): string {
    return { PUBLIC: 'Pública', PRIVATE: 'Privada', HIDDEN: 'Oculta' }[value];
  }

  protected roleLabel(role: CommunityRole): string {
    return { OWNER: 'Propietario', COMMUNITY_ADMIN: 'Administrador',
      COMMUNITY_MODERATOR: 'Moderador', MEMBER: 'Miembro' }[role];
  }
}

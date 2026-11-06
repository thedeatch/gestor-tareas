import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';

import {
  CommunityAccess,
  CommunityDetail,
  CommunityRole,
  CommunityVisibility
} from '../../models/community.model';
import { CommunityService } from '../../services/community';

@Component({
  selector: 'app-community-detail-page',
  imports: [RouterLink, ReactiveFormsModule, MatButtonModule, MatCardModule,
    MatIconModule, MatFormFieldModule, MatInputModule, MatSnackBarModule],
  templateUrl: './community-detail-page.html',
  styleUrl: './community-detail-page.css'
})
export class CommunityDetailPage {
  private readonly route = inject(ActivatedRoute);
  private readonly service = inject(CommunityService);
  private readonly snackBar = inject(MatSnackBar);
  protected readonly community = signal<CommunityDetail | null>(null);
  protected readonly joinRequests = signal<CommunityAccess[]>([]);
  protected readonly isLoading = signal(true);
  protected readonly isInviting = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly invitationEmail = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, Validators.email]
  });

  constructor() { this.load(); }

  protected load(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isInteger(id) || id < 1) {
      this.error.set('El grupo solicitado no es válido.');
      this.isLoading.set(false);
      return;
    }
    this.isLoading.set(true);
    this.service.findById(id).subscribe({
      next: community => {
        this.community.set(community);
        this.isLoading.set(false);
        if (this.canManage(community.currentUserRole)) this.loadJoinRequests(id);
      },
      error: () => { this.error.set('No fue posible abrir el grupo.'); this.isLoading.set(false); }
    });
  }

  protected canManage(role: CommunityRole): boolean {
    return role === 'OWNER' || role === 'COMMUNITY_ADMIN'
      || role === 'COMMUNITY_MODERATOR';
  }

  protected invite(): void {
    const group = this.community();
    if (!group || this.invitationEmail.invalid) {
      this.invitationEmail.markAsTouched();
      return;
    }
    this.isInviting.set(true);
    this.service.invite(group.id, this.invitationEmail.value).subscribe({
      next: () => {
        this.isInviting.set(false);
        this.invitationEmail.reset('');
        this.snackBar.open('Invitación enviada.', 'Cerrar', { duration: 2500 });
      },
      error: () => {
        this.isInviting.set(false);
        this.snackBar.open('No fue posible invitar a ese usuario.', 'Cerrar',
          { duration: 3000 });
      }
    });
  }

  protected resolveRequest(request: CommunityAccess, approve: boolean): void {
    const group = this.community();
    if (!group) return;
    this.service.resolveJoinRequest(group.id, request.id, approve).subscribe({
      next: () => {
        this.joinRequests.update(items =>
          items.filter(item => item.id !== request.id));
        if (approve) this.load();
        this.snackBar.open(approve ? 'Solicitud aprobada.' : 'Solicitud rechazada.',
          'Cerrar', { duration: 2500 });
      },
      error: () => this.snackBar.open('No fue posible responder la solicitud.',
        'Cerrar', { duration: 3000 })
    });
  }

  private loadJoinRequests(groupId: number): void {
    this.service.findJoinRequests(groupId).subscribe({
      next: requests => this.joinRequests.set(requests),
      error: () => this.joinRequests.set([])
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

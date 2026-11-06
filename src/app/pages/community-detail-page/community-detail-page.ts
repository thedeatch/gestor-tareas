import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';

import {
  CommunityAccess,
  CommunityDetail,
  CommunityMember,
  CommunityRole,
  CommunityVisibility
} from '../../models/community.model';
import { CommunityService } from '../../services/community';

@Component({
  selector: 'app-community-detail-page',
  imports: [RouterLink, ReactiveFormsModule, MatButtonModule, MatCardModule,
    MatIconModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatSnackBarModule],
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
  protected readonly selectedRoles = signal<Record<number, CommunityRole>>({});
  protected readonly savingRoleFor = signal<number | null>(null);
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
        this.selectedRoles.set(Object.fromEntries(
          community.members.map(member => [member.userId, member.role])
        ));
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

  protected canManageRoles(role: CommunityRole): boolean {
    return role === 'OWNER' || role === 'COMMUNITY_ADMIN';
  }

  protected canEditMember(
    currentUserRole: CommunityRole,
    memberRole: CommunityRole
  ): boolean {
    if (memberRole === 'OWNER') return false;
    if (currentUserRole === 'OWNER') return true;
    return currentUserRole === 'COMMUNITY_ADMIN'
      && memberRole !== 'COMMUNITY_ADMIN';
  }

  protected availableRoles(currentUserRole: CommunityRole): CommunityRole[] {
    return currentUserRole === 'OWNER'
      ? ['COMMUNITY_ADMIN', 'COMMUNITY_MODERATOR', 'MEMBER']
      : ['COMMUNITY_MODERATOR', 'MEMBER'];
  }

  protected selectRole(userId: number, role: CommunityRole): void {
    this.selectedRoles.update(roles => ({ ...roles, [userId]: role }));
  }

  protected saveRole(member: CommunityMember): void {
    const group = this.community();
    const role = this.selectedRoles()[member.userId];
    if (!group || !role || role === member.role || this.savingRoleFor()) return;
    this.savingRoleFor.set(member.userId);
    this.service.updateMemberRole(group.id, member.userId, role).subscribe({
      next: updated => {
        this.savingRoleFor.set(null);
        this.community.update(current => current ? ({
          ...current,
          members: current.members.map(item =>
            item.userId === updated.userId ? updated : item)
        }) : current);
        this.snackBar.open('Rol actualizado correctamente.', 'Cerrar',
          { duration: 2500 });
      },
      error: () => {
        this.savingRoleFor.set(null);
        this.selectRole(member.userId, member.role);
        this.snackBar.open('No fue posible actualizar el rol.', 'Cerrar',
          { duration: 3000 });
      }
    });
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

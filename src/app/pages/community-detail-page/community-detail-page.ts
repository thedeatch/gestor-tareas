import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';

import {
  CommunityAccess,
  CommunityDetail,
  CommunityMember,
  CommunityPost,
  CommunityRole,
  CommunityVisibility
} from '../../models/community.model';
import { CommunityService } from '../../services/community';
import {
  ConfirmDialog,
  ConfirmDialogData
} from '../../components/confirm-dialog/confirm-dialog';

import { PostComments } from '../../components/post-comments/post-comments';

@Component({
  selector: 'app-community-detail-page',
  imports: [RouterLink, ReactiveFormsModule, DatePipe, MatButtonModule, MatCardModule,
    MatIconModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatSnackBarModule, MatDialogModule, PostComments],
  templateUrl: './community-detail-page.html',
  styleUrl: './community-detail-page.css'
})
export class CommunityDetailPage {
  private readonly route = inject(ActivatedRoute);
  private readonly service = inject(CommunityService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);
  protected readonly community = signal<CommunityDetail | null>(null);
  protected readonly joinRequests = signal<CommunityAccess[]>([]);
  protected readonly posts = signal<CommunityPost[]>([]);
  protected readonly postsLoading = signal(true);
  protected readonly savingPost = signal(false);
  protected readonly editingPostId = signal<number | null>(null);
  protected readonly isLoading = signal(true);
  protected readonly isInviting = signal(false);
  protected readonly selectedRoles = signal<Record<number, CommunityRole>>({});
  protected readonly savingRoleFor = signal<number | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly invitationEmail = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, Validators.email]
  });
  protected readonly postContent = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, Validators.maxLength(2000)]
  });
  protected readonly postTitle = new FormControl('', {
    nonNullable: true,
    validators: [Validators.maxLength(150)]
  });
  protected readonly editContent = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, Validators.maxLength(2000)]
  });
  protected readonly editTitle = new FormControl('', {
    nonNullable: true,
    validators: [Validators.maxLength(150)]
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
        this.loadPosts(id);
        if (this.canManage(community.currentUserRole)) this.loadJoinRequests(id);
      },
      error: () => { this.error.set('No fue posible abrir el grupo.'); this.isLoading.set(false); }
    });
  }

  protected publish(event?: Event): void {
    event?.preventDefault();
    const group = this.community();
    const needsTitle = this.posts().length === 0;
    if (needsTitle && !this.postTitle.value.trim()) {
      this.postTitle.setErrors({ required: true });
    }
    if (!group || this.postTitle.invalid || this.postContent.invalid
        || this.savingPost()) {
      this.postTitle.markAsTouched();
      this.postContent.markAsTouched();
      return;
    }
    this.savingPost.set(true);
    this.service.createPost(group.id,
      needsTitle ? this.postTitle.value : null,
      this.postContent.value).subscribe({
      next: post => {
        this.posts.update(items => [post, ...items]);
        this.postTitle.reset('');
        this.postContent.reset('');
        this.savingPost.set(false);
        this.snackBar.open('Publicación creada.', 'Cerrar', { duration: 2500 });
      },
      error: () => {
        this.savingPost.set(false);
        this.snackBar.open('No fue posible crear la publicación.', 'Cerrar',
          { duration: 3000 });
      }
    });
  }

  protected beginEdit(post: CommunityPost): void {
    this.editingPostId.set(post.id);
    this.editTitle.setValue(post.title ?? '');
    this.editContent.setValue(post.content);
  }

  protected cancelEdit(): void {
    this.editingPostId.set(null);
    this.editTitle.reset('');
    this.editContent.reset('');
  }

  protected savePost(post: CommunityPost): void {
    const group = this.community();
    if (post.title !== null && !this.editTitle.value.trim()) {
      this.editTitle.setErrors({ required: true });
      this.editTitle.markAsTouched();
    }
    if (!group || this.editTitle.invalid || this.editContent.invalid
        || this.savingPost()) return;
    this.savingPost.set(true);
    this.service.updatePost(group.id, post.id,
      post.title !== null ? this.editTitle.value : null,
      this.editContent.value).subscribe({
      next: updated => {
        this.posts.update(items => items.map(item =>
          item.id === updated.id ? updated : item));
        this.cancelEdit();
        this.savingPost.set(false);
        this.snackBar.open('Publicación actualizada.', 'Cerrar', { duration: 2500 });
      },
      error: () => {
        this.savingPost.set(false);
        this.snackBar.open('No fue posible actualizar la publicación.', 'Cerrar',
          { duration: 3000 });
      }
    });
  }

  protected confirmDelete(post: CommunityPost): void {
    const group = this.community();
    if (!group) return;
    const data: ConfirmDialogData = {
      title: 'Eliminar publicación',
      itemLabel: 'la publicación de',
      itemName: post.authorName,
      confirmLabel: 'Eliminar'
    };
    this.dialog.open(ConfirmDialog, { data, width: '420px' })
      .afterClosed().subscribe(confirmed => {
        if (!confirmed) return;
        this.service.deletePost(group.id, post.id).subscribe({
          next: () => {
            this.posts.update(items => items.filter(item => item.id !== post.id));
            this.snackBar.open('Publicación eliminada.', 'Cerrar',
              { duration: 2500 });
          },
          error: () => this.snackBar.open(
            'No fue posible eliminar la publicación.', 'Cerrar',
            { duration: 3000 })
        });
      });
  }

  private loadPosts(groupId: number): void {
    this.postsLoading.set(true);
    this.service.findPosts(groupId).subscribe({
      next: posts => { this.posts.set(posts); this.postsLoading.set(false); },
      error: () => { this.posts.set([]); this.postsLoading.set(false); }
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

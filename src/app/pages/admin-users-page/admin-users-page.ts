import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { finalize } from 'rxjs';

import { AdminUser } from '../../models/admin.model';
import { UserRole } from '../../models/auth.model';
import { AdminService } from '../../services/admin';
import { AuthService } from '../../services/auth';

@Component({
  selector: 'app-admin-users-page',
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule
  ],
  templateUrl: './admin-users-page.html',
  styleUrl: './admin-users-page.css'
})
export class AdminUsersPage {
  private readonly adminService = inject(AdminService);
  private readonly authService = inject(AuthService);

  protected readonly roles: UserRole[] = ['USER', 'MODERATOR', 'ADMIN'];
  protected readonly currentEmail = this.authService.userEmail;
  protected readonly users = signal<AdminUser[]>([]);
  protected readonly roleDrafts = signal<Record<number, UserRole>>({});
  protected readonly search = signal('');
  protected readonly page = signal(0);
  protected readonly totalPages = signal(0);
  protected readonly totalElements = signal(0);
  protected readonly isLoading = signal(false);
  protected readonly savingUserId = signal<number | null>(null);
  protected readonly message = signal<string | null>(null);
  protected readonly errorMessage = signal<string | null>(null);

  constructor() {
    this.loadUsers();
  }

  protected searchUsers(): void {
    this.loadUsers(0);
  }

  protected clearSearch(): void {
    this.search.set('');
    this.loadUsers(0);
  }

  protected previousPage(): void {
    if (this.page() > 0) {
      this.loadUsers(this.page() - 1);
    }
  }

  protected nextPage(): void {
    if (this.page() + 1 < this.totalPages()) {
      this.loadUsers(this.page() + 1);
    }
  }

  protected isCurrentUser(user: AdminUser): boolean {
    return user.email.toLowerCase() === this.currentEmail()?.toLowerCase();
  }

  protected roleLabel(role: UserRole): string {
    return {
      USER: 'Usuario',
      MODERATOR: 'Moderador',
      ADMIN: 'Administrador'
    }[role];
  }

  protected setRoleDraft(userId: number, role: UserRole): void {
    this.roleDrafts.update(values => ({
      ...values,
      [userId]: role
    }));
  }

  protected updateRole(user: AdminUser): void {
    const newRole = this.roleDrafts()[user.id];
    if (!newRole || newRole === user.role || this.isCurrentUser(user)) {
      return;
    }

    this.message.set(null);
    this.errorMessage.set(null);
    this.savingUserId.set(user.id);
    this.adminService.updateRole(user.id, newRole).pipe(
      finalize(() => this.savingUserId.set(null))
    ).subscribe({
      next: updatedUser => {
        this.users.update(users => users.map(current =>
          current.id === updatedUser.id ? updatedUser : current
        ));
        this.roleDrafts.update(drafts => ({
          ...drafts,
          [updatedUser.id]: updatedUser.role
        }));
        this.message.set(`Rol de ${updatedUser.name} actualizado.`);
      },
      error: error => this.errorMessage.set(
        error.status === 409
          ? 'No es posible retirar el acceso del último administrador.'
          : 'No fue posible actualizar el rol.'
      )
    });
  }

  private loadUsers(page = 0): void {
    this.message.set(null);
    this.errorMessage.set(null);
    this.isLoading.set(true);
    this.adminService.findUsers(this.search().trim(), page).pipe(
      finalize(() => this.isLoading.set(false))
    ).subscribe({
      next: response => {
        this.users.set(response.content);
        this.page.set(response.number);
        this.totalPages.set(response.totalPages);
        this.totalElements.set(response.totalElements);
        this.roleDrafts.set(Object.fromEntries(
          response.content.map(user => [user.id, user.role])
        ));
      },
      error: () => this.errorMessage.set(
        'No fue posible cargar los usuarios.'
      )
    });
  }
}

import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { finalize } from 'rxjs';

import {
  AdminAuditAction,
  AdminAuditEntry
} from '../../models/admin.model';
import { AdminService } from '../../services/admin';

@Component({
  selector: 'app-admin-audit-page',
  imports: [
    CommonModule,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './admin-audit-page.html',
  styleUrl: './admin-audit-page.css'
})
export class AdminAuditPage {
  private readonly adminService = inject(AdminService);

  protected readonly entries = signal<AdminAuditEntry[]>([]);
  protected readonly page = signal(0);
  protected readonly totalPages = signal(0);
  protected readonly totalElements = signal(0);
  protected readonly isLoading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  constructor() {
    this.loadEntries();
  }

  protected previousPage(): void {
    if (this.page() > 0) this.loadEntries(this.page() - 1);
  }

  protected nextPage(): void {
    if (this.page() + 1 < this.totalPages()) {
      this.loadEntries(this.page() + 1);
    }
  }

  protected actionLabel(action: AdminAuditAction): string {
    return {
      ROLE_CHANGED: 'Cambio de rol',
      ACCOUNT_SUSPENDED: 'Cuenta suspendida',
      ACCOUNT_ACTIVATED: 'Cuenta reactivada'
    }[action];
  }

  protected valueLabel(value: string): string {
    return {
      USER: 'Usuario',
      MODERATOR: 'Moderador',
      ADMIN: 'Administrador',
      ACTIVE: 'Activa',
      SUSPENDED: 'Suspendida'
    }[value] ?? value;
  }

  private loadEntries(page = 0): void {
    this.errorMessage.set(null);
    this.isLoading.set(true);
    this.adminService.findAuditLog(page).pipe(
      finalize(() => this.isLoading.set(false))
    ).subscribe({
      next: response => {
        this.entries.set(response.content);
        this.page.set(response.number);
        this.totalPages.set(response.totalPages);
        this.totalElements.set(response.totalElements);
      },
      error: () => this.errorMessage.set(
        'No fue posible cargar el historial de auditoría.'
      )
    });
  }
}

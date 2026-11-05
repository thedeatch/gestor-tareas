import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import {
  MAT_DATE_LOCALE,
  provideNativeDateAdapter
} from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
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
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule
  ],
  providers: [
    provideNativeDateAdapter(),
    { provide: MAT_DATE_LOCALE, useValue: 'es-CL' }
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
  protected readonly search = signal('');
  protected readonly action = signal<AdminAuditAction | ''>('');
  protected readonly fromDate = signal<Date | null>(null);
  protected readonly toDate = signal<Date | null>(null);
  protected readonly maximumDate = new Date();

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

  protected applyFilters(): void {
    if (
      this.fromDate() && this.toDate() &&
      this.fromDate()!.getTime() > this.toDate()!.getTime()
    ) {
      this.errorMessage.set(
        'La fecha inicial no puede ser posterior a la fecha final.'
      );
      return;
    }
    this.loadEntries(0);
  }

  protected clearFilters(): void {
    this.search.set('');
    this.action.set('');
    this.fromDate.set(null);
    this.toDate.set(null);
    this.loadEntries(0);
  }

  protected hasFilters(): boolean {
    return Boolean(
      this.search().trim() || this.action() ||
      this.fromDate() || this.toDate()
    );
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
    this.adminService.findAuditLog(page, {
      search: this.search().trim(),
      action: this.action(),
      from: this.formatDate(this.fromDate()),
      to: this.formatDate(this.toDate())
    }).pipe(
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

  private formatDate(date: Date | null): string {
    if (!date) return '';
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}

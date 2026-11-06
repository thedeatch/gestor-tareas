import { DatePipe } from '@angular/common';
import { Component, inject, input, output, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { finalize } from 'rxjs';

import { getApiErrorMessage } from '../../models/api-error.model';
import { CommunityReport, CommunityReportsService, ReportResolution } from '../../services/community-reports';
import { AdminReasonDialog, AdminReasonDialogData } from '../admin-reason-dialog/admin-reason-dialog';

@Component({
  selector: 'app-community-reports',
  imports: [DatePipe, MatButtonModule, MatCardModule, MatDialogModule],
  templateUrl: './community-reports.html',
  styles: [`
    :host { display: block; margin-top: 1rem; }
    article { border-top: 1px solid var(--mat-sys-outline-variant); padding-block: .75rem; }
    blockquote, p { overflow-wrap: anywhere; white-space: pre-wrap; }
  `]
})
export class CommunityReports {
  readonly groupId = input.required<number>();
  readonly contentRemoved = output<void>();
  private readonly reports = inject(CommunityReportsService);
  private readonly dialog = inject(MatDialog);

  protected readonly items = signal<CommunityReport[] | null>(null);
  protected readonly busy = signal(false);
  protected readonly message = signal('');
  protected readonly error = signal('');

  protected load(): void {
    this.busy.set(true);
    this.error.set('');
    this.reports.pending(this.groupId()).pipe(finalize(() => this.busy.set(false))).subscribe({
      next: items => this.items.set(items),
      error: error => this.error.set(getApiErrorMessage(error, 'No se pudieron cargar los reportes.'))
    });
  }

  protected resolve(report: CommunityReport, resolution: ReportResolution): void {
    const remove = resolution === 'REMOVED';
    const data: AdminReasonDialogData = {
      title: remove ? 'Retirar contenido reportado' : 'Descartar reporte',
      message: remove
        ? 'El autor recibirá el motivo si el contenido es ajeno.'
        : 'El motivo quedará registrado en la resolución.',
      reasonLabel: 'Motivo de la resolución',
      reasonPlaceholder: 'Explica la decisión',
      confirmText: remove ? 'Retirar' : 'Descartar'
    };
    this.dialog.open<AdminReasonDialog, AdminReasonDialogData, string>(
      AdminReasonDialog, { data, width: '420px' }
    ).afterClosed().subscribe(reason => {
      if (!reason || this.busy()) return;
      this.busy.set(true);
      this.error.set('');
      this.message.set('');
      this.reports.resolve(this.groupId(), report.id, resolution, reason)
        .pipe(finalize(() => this.busy.set(false))).subscribe({
          next: () => {
            this.items.update(items => items?.filter(item => item.id !== report.id) ?? []);
            this.message.set(remove ? 'Contenido retirado.' : 'Reporte descartado.');
            if (remove) this.contentRemoved.emit();
          },
          error: error => this.error.set(getApiErrorMessage(error, 'No se pudo resolver el reporte.'))
        });
    });
  }
}
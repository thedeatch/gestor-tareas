import { Component, inject, input, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { finalize } from 'rxjs';

import { getApiErrorMessage } from '../../models/api-error.model';
import { CommunityReportsService } from '../../services/community-reports';
import { AdminReasonDialog, AdminReasonDialogData } from '../admin-reason-dialog/admin-reason-dialog';

@Component({
  selector: 'app-report-button',
  imports: [MatButtonModule, MatDialogModule, MatSnackBarModule],
  template: `<button mat-button type="button" [disabled]="sending()" (click)="report()">Reportar</button>`
})
export class ReportButton {
  readonly groupId = input.required<number>();
  readonly postId = input.required<number>();
  readonly commentId = input<number>();

  private readonly reports = inject(CommunityReportsService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  protected readonly sending = signal(false);

  protected report(): void {
    const data: AdminReasonDialogData = {
      title: this.commentId() ? 'Reportar comentario' : 'Reportar publicación',
      message: 'El equipo de moderación del grupo revisará tu reporte.',
      reasonLabel: 'Motivo del reporte',
      reasonPlaceholder: 'Explica qué debe revisar el equipo',
      confirmText: 'Enviar reporte'
    };
    this.dialog.open<AdminReasonDialog, AdminReasonDialogData, string>(
      AdminReasonDialog, { data, width: '420px' }
    ).afterClosed().subscribe(reason => {
      if (!reason || this.sending()) return;
      this.sending.set(true);
      this.reports.create(this.groupId(), this.postId(), this.commentId(), reason)
        .pipe(finalize(() => this.sending.set(false))).subscribe({
          next: () => this.snackBar.open('Reporte enviado.', 'Cerrar', { duration: 3000 }),
          error: error => this.snackBar.open(
            getApiErrorMessage(error, 'No se pudo enviar el reporte.'), 'Cerrar', { duration: 4000 })
        });
    });
  }
}
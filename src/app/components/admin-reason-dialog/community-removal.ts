import { MatDialog } from '@angular/material/dialog';
import { Observable } from 'rxjs';
import { AdminReasonDialog, AdminReasonDialogData } from './admin-reason-dialog';

export function confirmCommunityRemoval(
  dialog: MatDialog, content: 'publicación' | 'comentario', author: string
): Observable<string | undefined> {
  const data: AdminReasonDialogData = {
    title: `Retirar ${content}`,
    message: `Indica por qué retiras el contenido de ${author}. El autor recibirá el motivo.`,
    reasonLabel: 'Motivo de moderación',
    reasonPlaceholder: 'Escribe el motivo',
    confirmText: 'Retirar'
  };
  return dialog.open<AdminReasonDialog, AdminReasonDialogData, string>(
    AdminReasonDialog, { data, width: '420px' }
  ).afterClosed();
}
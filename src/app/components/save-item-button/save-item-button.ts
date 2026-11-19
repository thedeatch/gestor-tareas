import { Component, DestroyRef, effect, inject, input, output, untracked } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { getApiErrorMessage } from '../../models/api-error.model';
import { SavedItemKind } from '../../models/saved-item.model';
import { SavedItemsService } from '../../services/saved-items';

@Component({
  selector: 'app-save-item-button',
  imports: [MatButtonModule, MatIconModule],
  template: `
    <button mat-icon-button type="button" [class.compact]="compact()"
      [attr.aria-label]="label()" [title]="label()"
      [attr.aria-pressed]="service.isSaved(kind(), targetId())"
      [disabled]="disabled() || !service.groupStatus(groupId()) || service.groupStatus(groupId()) === 'loading' || service.isPending(kind(), targetId())"
      (click)="toggle()">
      <mat-icon>{{ service.isSaved(kind(), targetId()) ? 'bookmark' : 'bookmark_border' }}</mat-icon>
    </button>
  `,
  styles: [`
    :host { display: inline-block; }
    button[aria-pressed="true"] { color: var(--mat-sys-primary); }
    button.compact { width: 32px; height: 32px; padding: 4px; background: #000c; color: #fff; }
    button.compact[aria-pressed="true"] { color: #b8d6ff; }
    button.compact mat-icon { width: 20px; height: 20px; font-size: 20px; }
  `]
})
export class SaveItemButton {
  readonly groupId = input.required<number>();
  readonly targetId = input.required<number>();
  readonly kind = input.required<SavedItemKind>();
  readonly compact = input(false);
  readonly disabled = input(false);
  readonly changed = output<boolean>();
  protected readonly service = inject(SavedItemsService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly snackBar = inject(MatSnackBar);

  constructor() {
    effect(() => {
      const groupId = this.groupId();
      this.service.userEmail();
      untracked(() => this.service.loadGroup(groupId));
    });
  }

  protected label(): string {
    if (this.service.groupStatus(this.groupId()) === 'error') return 'Reintentar carga de guardados';
    return this.service.isSaved(this.kind(), this.targetId()) ? 'Quitar de mis guardados' : 'Guardar para después';
  }

  protected toggle(): void {
    if (this.disabled() || this.service.isPending(this.kind(), this.targetId())) return;
    if (this.service.groupStatus(this.groupId()) !== 'ready') {
      this.service.loadGroup(this.groupId());
      return;
    }
    this.service.setSaved(this.groupId(), this.kind(), this.targetId(),
      !this.service.isSaved(this.kind(), this.targetId()))
      .pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: saved => this.changed.emit(saved),
        error: error => this.snackBar.open(
          getApiErrorMessage(error, 'No se pudo actualizar el elemento guardado.'),
          'Cerrar', { duration: 5000 })
      });
  }
}

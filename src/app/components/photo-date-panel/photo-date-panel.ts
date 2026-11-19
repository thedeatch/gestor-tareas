import { DatePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, DestroyRef, effect, inject, input, output, signal, untracked } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { finalize } from 'rxjs';
import { getApiErrorMessage } from '../../models/api-error.model';
import { PhotoDateChange, PhotoDateInfo, createPhotoDateForm, photoDateInput, photoDateValue } from '../../models/photo-date.model';
import { PhotoDateFields } from '../photo-date-fields/photo-date-fields';

@Component({
  selector: 'app-photo-date-panel',
  imports: [DatePipe, ReactiveFormsModule, MatButtonModule, PhotoDateFields],
  template: `
    <p class="reference">
      @if (date().takenOn) { Tomada el {{ date().takenOn | date:'dd/MM/yyyy' }} }
      @else if (date().takenYear !== null) { Tomada en {{ date().takenYear }} }
      @else { Subida el {{ date().uploadedOn | date:'dd/MM/yyyy' }} }
      @if (date().years > 0) { · Recuerdo de {{ date().years }} {{ date().years === 1 ? 'año' : 'años' }} }
    </p>
    @if (date().canEdit) {
      <details>
        <summary>Opciones avanzadas de la foto</summary>
        <form [formGroup]="form" (ngSubmit)="save()">
          <p>Indica cuándo se tomó la foto para calcular sus recuerdos.</p>
          <app-photo-date-fields [form]="form" />
          @if (error()) { <p class="error" role="alert">{{ error() }}</p> }
          <button mat-button type="submit" [disabled]="busy() || form.invalid">
            {{ busy() ? 'Guardando…' : 'Guardar fecha' }}
          </button>
        </form>
      </details>
    }
    @if (message()) { <p role="status">{{ message() }}</p> }
  `,
  styles: [`
    :host { display: block; margin-top: .5rem; }
    .reference, summary, p { font-size: .85rem; color: var(--mat-sys-on-surface-variant); }
    .reference { text-align: center; }
    details { border-top: 1px solid var(--mat-sys-outline-variant); padding-top: .65rem; }
    summary { cursor: pointer; color: var(--mat-sys-primary); }
    form { padding: .5rem 0; max-width: 440px; margin: auto; }
    .error { color: var(--mat-sys-error); }
  `]
})
export class PhotoDatePanel {
  readonly date = input.required<PhotoDateInfo>();
  readonly imageId = input.required<number>();
  readonly imagesUrl = input.required<string>();
  readonly changed = output<PhotoDateChange>();
  protected readonly form = createPhotoDateForm();
  protected readonly busy = signal(false);
  protected readonly error = signal('');
  protected readonly message = signal('');
  private readonly http = inject(HttpClient);
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    effect(() => {
      const value = photoDateValue(this.date());
      untracked(() => this.form.reset(value));
    });
  }

  protected save(): void {
    if (this.busy() || this.form.invalid) return;
    const imageId = this.imageId();
    this.busy.set(true);
    this.form.disable();
    this.error.set('');
    this.message.set('');
    this.http.patch<{ date: PhotoDateInfo }>(`${this.imagesUrl()}/${imageId}/date`,
      photoDateInput(this.form.getRawValue())).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => { this.busy.set(false); this.form.enable(); })
    ).subscribe({
      next: result => {
        this.changed.emit({ imageId, date: result.date });
        this.message.set('Fecha de la foto actualizada.');
      },
      error: error => this.error.set(getApiErrorMessage(error, 'No se pudo guardar la fecha de la foto.'))
    });
  }
}

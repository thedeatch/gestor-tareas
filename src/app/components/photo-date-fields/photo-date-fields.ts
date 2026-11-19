import { Component, input } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { PhotoDateForm } from '../../models/photo-date.model';
import { localDate } from '../../models/task-filter.model';

@Component({
  selector: 'app-photo-date-fields',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  template: `
    <div [formGroup]="form()">
      <mat-form-field appearance="outline">
        <mat-label>Fecha para el recuerdo</mat-label>
        <mat-select formControlName="mode">
          <mat-option value="UPLOAD">Usar fecha de subida</mat-option>
          <mat-option value="DATE">Sé la fecha exacta de la foto</mat-option>
          <mat-option value="YEAR">Solo sé el año de la foto</mat-option>
        </mat-select>
      </mat-form-field>
      @if (form().controls.mode.value === 'DATE') {
        <mat-form-field appearance="outline">
          <mat-label>¿Cuándo se tomó?</mat-label>
          <input matInput type="date" formControlName="takenOn" [max]="today">
        </mat-form-field>
      }
      @if (form().controls.mode.value === 'YEAR') {
        <mat-form-field appearance="outline">
          <mat-label>Año en que se tomó</mat-label>
          <input matInput type="number" formControlName="takenYear" min="1" [max]="year" step="1" placeholder="2016">
        </mat-form-field>
      }
      @if (form().invalid) {
        <p class="error" role="alert">Indica una fecha o un año válido que no sea futuro.</p>
      }
    </div>
  `,
  styles: [`
    :host, mat-form-field { display: block; width: 100%; }
    div { display: grid; gap: .5rem; }
    .error { color: var(--mat-sys-error); font-size: .85rem; margin: 0 0 .5rem; }
  `]
})
export class PhotoDateFields {
  readonly form = input.required<PhotoDateForm>();
  protected readonly today = localDate(new Date());
  protected readonly year = new Date().getFullYear();
}

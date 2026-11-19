import { AbstractControl, FormControl, FormGroup, ValidationErrors } from '@angular/forms';
import { localDate } from './task-filter.model';

export interface PhotoDateInfo {
  uploadedOn: string;
  takenOn: string | null;
  takenYear: number | null;
  years: number;
  anniversary: boolean;
  canEdit: boolean;
}

export interface PhotoDateInput {
  takenOn: string | null;
  takenYear: number | null;
}

export interface PhotoDateValue {
  mode: 'UPLOAD' | 'DATE' | 'YEAR';
  takenOn: string;
  takenYear: string;
}

export interface PhotoDateChange {
  imageId: number;
  date: PhotoDateInfo;
}

export function photoDateValue(date?: PhotoDateInfo): PhotoDateValue {
  return {
    mode: date?.takenOn ? 'DATE' : date?.takenYear != null ? 'YEAR' : 'UPLOAD',
    takenOn: date?.takenOn ?? '',
    takenYear: date?.takenYear != null ? String(date.takenYear) : ''
  };
}

export function photoDateInput(value: PhotoDateValue): PhotoDateInput {
  return {
    takenOn: value.mode === 'DATE' ? value.takenOn : null,
    takenYear: value.mode === 'YEAR' ? Number(value.takenYear) : null
  };
}

export function createPhotoDateForm() {
  return new FormGroup({
    mode: new FormControl<PhotoDateValue['mode']>('UPLOAD', { nonNullable: true }),
    takenOn: new FormControl('', { nonNullable: true }),
    takenYear: new FormControl('', { nonNullable: true })
  }, { validators: validatePhotoDate });
}

function validatePhotoDate(control: AbstractControl): ValidationErrors | null {
  const value = control.value as PhotoDateValue;
  if (value.mode === 'DATE') {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(value.takenOn) || value.takenOn > localDate(new Date())
        || Number(value.takenOn.slice(0, 4)) < 1) return { photoDate: true };
  }
  if (value.mode === 'YEAR') {
    const year = Number(value.takenYear);
    if (!Number.isInteger(year) || year < 1 || year > new Date().getFullYear()) return { photoYear: true };
  }
  return null;
}

export type PhotoDateForm = ReturnType<typeof createPhotoDateForm>;

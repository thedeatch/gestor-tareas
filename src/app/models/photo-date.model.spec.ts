import { createPhotoDateForm, photoDateInput, photoDateValue } from './photo-date.model';

describe('Photo date form', () => {
  it('uses the upload date by default and sends only the chosen precision', () => {
    const form = createPhotoDateForm();
    expect(form.valid).toBe(true);
    expect(photoDateInput(form.getRawValue())).toEqual({ takenOn: null, takenYear: null });
    form.setValue({ mode: 'YEAR', takenOn: '2010-01-01', takenYear: '2016' });
    expect(photoDateInput(form.getRawValue())).toEqual({ takenOn: null, takenYear: 2016 });
    form.setValue({ mode: 'DATE', takenOn: '2010-01-01', takenYear: '2016' });
    expect(photoDateInput(form.getRawValue())).toEqual({ takenOn: '2010-01-01', takenYear: null });
  });

  it('rejects empty, fractional and future years while permitting upload-date fallback', () => {
    const form = createPhotoDateForm();
    for (const year of ['', '0', '2016.5', '9999']) {
      form.setValue({ mode: 'YEAR', takenOn: '', takenYear: year });
      expect(form.invalid).toBe(true);
    }
    form.controls.mode.setValue('UPLOAD');
    expect(form.valid).toBe(true);
    expect(photoDateValue({
      uploadedOn: '2026-11-18', takenOn: null, takenYear: 2016, years: 10, anniversary: false, canEdit: true
    }).mode).toBe('YEAR');
  });
});

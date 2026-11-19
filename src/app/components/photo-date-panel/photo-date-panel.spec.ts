import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { PhotoDatePanel } from './photo-date-panel';

describe('PhotoDatePanel', () => {
  let http: HttpTestingController;
  const date = {
    uploadedOn: '2026-11-18', takenOn: null, takenYear: 2016,
    years: 10, anniversary: false, canEdit: true
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PhotoDatePanel], providers: [provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function create(canEdit = true) {
    const fixture = TestBed.createComponent(PhotoDatePanel);
    fixture.componentRef.setInput('date', { ...date, canEdit });
    fixture.componentRef.setInput('imageId', 3);
    fixture.componentRef.setInput('imagesUrl', '/api/communities/2/albums/1/images');
    fixture.detectChanges();
    return fixture;
  }

  it('saves a corrected year from the advanced form and emits the updated metadata', () => {
    const fixture = create();
    const changes: unknown[] = [];
    fixture.componentInstance.changed.subscribe(value => changes.push(value));
    const details = fixture.nativeElement.querySelector('details') as HTMLDetailsElement;
    details.open = true;
    const input = fixture.nativeElement.querySelector('input[type="number"]') as HTMLInputElement;
    input.value = '2010';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    fixture.nativeElement.querySelector('button[type="submit"]').click();
    const request = http.expectOne({ method: 'PATCH', url: '/api/communities/2/albums/1/images/3/date' });
    expect(request.request.body).toEqual({ takenOn: null, takenYear: 2010 });
    request.flush({ date: { ...date, takenYear: 2010, years: 16 } });
    fixture.detectChanges();
    expect(changes).toEqual([{ imageId: 3, date: { ...date, takenYear: 2010, years: 16 } }]);
    expect(fixture.nativeElement.textContent).toContain('Fecha de la foto actualizada.');
  });

  it('preserves a failed edit and keeps the advanced controls hidden for viewers', () => {
    const fixture = create();
    fixture.nativeElement.querySelector('details').open = true;
    fixture.nativeElement.querySelector('button[type="submit"]').click();
    http.expectOne('/api/communities/2/albums/1/images/3/date')
      .flush({}, { status: 500, statusText: 'Error' });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('input').value).toBe('2016');
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('No se pudo guardar');
    fixture.componentRef.setInput('date', { ...date, canEdit: false });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('details')).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Tomada en 2016');
  });
});

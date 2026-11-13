import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { GalleryImageShareDialog } from './gallery-image-share-dialog';

describe('GalleryImageShareDialog', () => {
  it('creates and revokes a public link for only the selected image', async () => {
    await TestBed.configureTestingModule({
      imports: [GalleryImageShareDialog],
      providers: [
        provideHttpClient(), provideHttpClientTesting(),
        { provide: MatDialogRef, useValue: { close: () => {} } },
        { provide: MAT_DIALOG_DATA, useValue: { imageName: 'foto.png', url: '/api/images/7/share' } }
      ]
    }).compileComponents();
    const http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(GalleryImageShareDialog);
    fixture.detectChanges();
    const root = fixture.nativeElement as HTMLElement;

    (root.querySelector('button mat-icon')?.closest('button') as HTMLButtonElement).click();
    http.expectOne({ method: 'POST', url: '/api/images/7/share' }).flush({ token: 'token-publico' });
    fixture.detectChanges();
    expect((root.querySelector('#public-image-url') as HTMLInputElement).value)
      .toContain('/foto-compartida/token-publico');
    expect(root.textContent).toContain('no muestra el resto del álbum');

    const revoke = [...root.querySelectorAll('button')]
      .find(button => button.textContent?.includes('Desactivar enlace')) as HTMLButtonElement;
    revoke.click();
    http.expectOne({ method: 'DELETE', url: '/api/images/7/share' }).flush(null);
    fixture.detectChanges();
    expect(root.textContent).toContain('El enlace público fue desactivado');
    http.verify();
  });
});

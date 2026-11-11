import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { GalleryImageDialog } from './gallery-image-dialog';
import { provideRouter } from '@angular/router';

describe('GalleryImageDialog', () => {
  it('expands the photo, preserves the comment draft, and loads comments for the next photo', async () => {
    await TestBed.configureTestingModule({
      imports: [GalleryImageDialog],
      providers: [
        provideHttpClient(), provideHttpClientTesting(), provideRouter([]),
        { provide: MatDialogRef, useValue: { close: () => {} } },
        {
          provide: MAT_DIALOG_DATA,
          useValue: {
            albumTitle: 'Encuentro', initialIndex: 0, imagesUrl: '/api/communities/2/albums/1/images',
            images: [
              { id: 3, name: 'Primera foto', url: '/primera.png' },
              { id: 4, name: 'Segunda foto', url: '/segunda.png' }
            ]
          }
        }
      ]
    }).compileComponents();
    const http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(GalleryImageDialog);
    fixture.detectChanges();
    http.expectOne('/api/communities/2/albums/1/images/3/comments').flush([]);
    fixture.detectChanges();
    const root = fixture.nativeElement as HTMLElement;
    const textarea = root.querySelector('textarea')!;
    textarea.value = 'Mi comentario pendiente';
    textarea.dispatchEvent(new Event('input'));
    (root.querySelector('.image-button') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(root.querySelector('mat-dialog-content')?.classList.contains('image-only')).toBe(true);
    expect(root.querySelector('#image-comments')?.hasAttribute('hidden')).toBe(true);
    (root.querySelector('button[aria-controls="image-comments"]') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(root.querySelector('#image-comments')?.hasAttribute('hidden')).toBe(false);
    expect(root.querySelector('textarea')?.value).toBe('Mi comentario pendiente');
    (root.querySelector('[aria-label="Imagen siguiente"]') as HTMLButtonElement).click();
    fixture.detectChanges();
    http.expectOne('/api/communities/2/albums/1/images/4/comments').flush([]);
    fixture.detectChanges();
    expect(root.querySelector('.image-button img')?.getAttribute('alt')).toBe('Segunda foto');
    expect(root.querySelector('textarea')?.value).toBe('');
    http.verify();
  });
});

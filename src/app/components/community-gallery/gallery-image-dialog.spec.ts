import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { GalleryImageDialog } from './gallery-image-dialog';
import { provideRouter } from '@angular/router';

describe('GalleryImageDialog', () => {
  it('loads metadata at page boundaries, wraps to the first photo and preserves the current page on failure', async () => {
    const first = [{ id: 3, name: 'Una', url: '/3.png', likeCount: 0, liked: false }, { id: 4, name: 'Otra', url: '/4.png', likeCount: 0, liked: false }];
    await TestBed.configureTestingModule({ imports: [GalleryImageDialog], providers: [
      provideHttpClient(), provideHttpClientTesting(), provideRouter([]),
      { provide: MatDialogRef, useValue: { close: () => {} } },
      { provide: MAT_DIALOG_DATA, useValue: {
        groupId: 2, albumTitle: 'Paginado', initialIndex: 1, imagesUrl: '/api/communities/2/albums/1/images',
        images: first, page: 0, pageSize: 2, total: 3
      } }
    ] }).compileComponents();
    const http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(GalleryImageDialog);
    fixture.detectChanges();
    http.expectOne('/api/communities/2/albums/1/images/4/comments').flush([]);
    fixture.componentInstance.next();
    http.expectOne('/api/communities/2/albums/1/images/page?page=1').flush({
      items: [{ id: 5, name: 'Última', url: '/5.png', likeCount: 0, liked: false }], total: 3, page: 1, size: 2
    });
    fixture.detectChanges(); http.expectOne('/api/communities/2/albums/1/images/5/comments').flush([]);
    expect(fixture.componentInstance.position()).toBe(3);
    fixture.componentInstance.next();
    http.expectOne('/api/communities/2/albums/1/images/page?page=0').flush({}, { status: 503, statusText: 'Error' });
    expect(fixture.componentInstance.position()).toBe(3);
    fixture.componentInstance.next();
    http.expectOne('/api/communities/2/albums/1/images/page?page=0').flush({ items: first, total: 3, page: 0, size: 2 });
    fixture.detectChanges(); http.expectOne('/api/communities/2/albums/1/images/3/comments').flush([]);
    expect(fixture.componentInstance.position()).toBe(1);
    fixture.componentInstance.previous();
    const pending = http.expectOne('/api/communities/2/albums/1/images/page?page=1');
    fixture.destroy(); expect(pending.cancelled).toBe(true);
    http.verify();
  });

  it('fetches only the selected original and cancels it when switching photos', async () => {
    await TestBed.configureTestingModule({ imports: [GalleryImageDialog], providers: [
      provideHttpClient(), provideHttpClientTesting(), provideRouter([]),
      { provide: MatDialogRef, useValue: { close: () => {} } },
      { provide: MAT_DIALOG_DATA, useValue: {
        groupId: 2, albumTitle: 'Grande', initialIndex: 0, imagesUrl: '/api/communities/2/albums/1/images',
        images: [{ id: 3, name: 'Una', likeCount: 0, liked: false }, { id: 4, name: 'Otra', likeCount: 0, liked: false }]
      } }
    ] }).compileComponents();
    const http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(GalleryImageDialog);
    const create = vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:original');
    const revoke = vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => {});
    try {
      fixture.detectChanges();
      http.expectOne('/api/communities/2/albums/1/images/3/comments').flush([]);
      const old = http.expectOne('/api/communities/2/albums/1/images/3');
      http.expectNone('/api/communities/2/albums/1/images/4');
      fixture.componentInstance.next(); fixture.detectChanges();
      expect(old.cancelled).toBe(true);
      http.expectOne('/api/communities/2/albums/1/images/4/comments').flush([]);
      http.expectOne('/api/communities/2/albums/1/images/4').flush(new Blob());
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('.image-button img').src).toBe('blob:original');
      fixture.destroy(); expect(revoke).toHaveBeenCalledWith('blob:original');
      http.verify();
    } finally { create.mockRestore(); revoke.mockRestore(); }
  });

  it('expands the photo, preserves the comment draft, and loads comments for the next photo', async () => {
    await TestBed.configureTestingModule({
      imports: [GalleryImageDialog],
      providers: [
        provideHttpClient(), provideHttpClientTesting(), provideRouter([]),
        { provide: MatDialogRef, useValue: { close: () => {} } },
        {
          provide: MAT_DIALOG_DATA,
          useValue: {
            groupId: 2, albumTitle: 'Encuentro', initialIndex: 0, imagesUrl: '/api/communities/2/albums/1/images',
            images: [
              { id: 3, name: 'Primera foto', url: '/primera.png', likeCount: 0, liked: false },
              { id: 4, name: 'Segunda foto', url: '/segunda.png', likeCount: 0, liked: false }
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
    (root.querySelector('app-gallery-image-like button') as HTMLButtonElement).click();
    const like = http.expectOne({ method: 'PUT', url: '/api/communities/2/albums/1/images/3/like' });
    (root.querySelector('[aria-label="Imagen siguiente"]') as HTMLButtonElement).click();
    fixture.detectChanges();
    http.expectOne('/api/communities/2/albums/1/images/4/comments').flush([]);
    fixture.detectChanges();
    expect(root.querySelector('.image-button img')?.getAttribute('alt')).toBe('Segunda foto');
    expect(root.querySelector('textarea')?.value).toBe('');
    like.flush({ likeCount: 1, liked: true });
    fixture.detectChanges();
    expect(root.querySelector('app-gallery-image-like button')?.getAttribute('aria-pressed')).toBe('false');
    (root.querySelector('[aria-label="Imagen anterior"]') as HTMLButtonElement).click();
    fixture.detectChanges();
    http.expectOne('/api/communities/2/albums/1/images/3/comments').flush([]);
    fixture.detectChanges();
    expect(root.querySelector('app-gallery-image-like button')?.getAttribute('aria-pressed')).toBe('true');
    (root.querySelector('app-gallery-image-like button') as HTMLButtonElement).click();
    http.expectOne({ method: 'DELETE', url: '/api/communities/2/albums/1/images/3/like' })
      .flush({ likeCount: 0, liked: false });
    fixture.detectChanges();
    expect(root.querySelector('app-gallery-image-like button')?.getAttribute('aria-pressed')).toBe('false');
    http.verify();
  });
});

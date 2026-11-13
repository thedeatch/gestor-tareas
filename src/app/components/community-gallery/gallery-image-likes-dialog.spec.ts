import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { provideRouter } from '@angular/router';
import { GalleryImageLikesDialog } from './gallery-image-likes-dialog';

describe('GalleryImageLikesDialog', () => {
  it('lists the people who liked the image and links each one to their public profile', async () => {
    await TestBed.configureTestingModule({
      imports: [GalleryImageLikesDialog],
      providers: [
        provideHttpClient(), provideHttpClientTesting(), provideRouter([]),
        { provide: MatDialogRef, useValue: { close: () => {} } },
        {
          provide: MAT_DIALOG_DATA,
          useValue: { imageName: 'Foto del encuentro', url: '/api/communities/2/albums/1/images/3/likes' }
        }
      ]
    }).compileComponents();

    const http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(GalleryImageLikesDialog);
    fixture.detectChanges();
    http.expectOne('/api/communities/2/albums/1/images/3/likes').flush([
      { userId: 7, name: 'Ana', hasAvatar: false }
    ]);
    fixture.detectChanges();

    const profileLink = fixture.nativeElement.querySelector('a') as HTMLAnchorElement;
    expect(profileLink.textContent).toContain('Ana');
    expect(profileLink.getAttribute('href')).toBe('/usuarios/7');
    expect(fixture.nativeElement.textContent).toContain('Ver perfil');
    http.verify();
  });
});

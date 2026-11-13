import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ActivatedRoute } from '@angular/router';
import { SharedGalleryImagePage } from './shared-gallery-image-page';

describe('SharedGalleryImagePage', () => {
  it('shows one publicly shared image without requiring authentication', async () => {
    await TestBed.configureTestingModule({
      imports: [SharedGalleryImagePage],
      providers: [
        provideHttpClient(), provideHttpClientTesting(), provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'public-token' } } } }
      ]
    }).compileComponents();
    const http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(SharedGalleryImagePage);
    fixture.detectChanges();
    http.expectOne('/api/public/gallery-images/public-token').flush({
      name: 'foto.png', albumTitle: 'Encuentro', communityName: 'Grupo Angular'
    });
    fixture.detectChanges();
    const root = fixture.nativeElement as HTMLElement;
    expect(root.textContent).toContain('Grupo Angular');
    expect(root.textContent).toContain('Encuentro');
    expect(root.querySelector('img')?.getAttribute('src'))
      .toBe('/api/public/gallery-images/public-token/content');
    http.verify();
  });
});

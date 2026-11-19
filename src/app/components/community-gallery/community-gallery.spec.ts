import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { CommunityGallery } from './community-gallery';

describe('CommunityGallery upload dates', () => {
  it('pages image tiles without downloading any originals on album opening', async () => {
    await TestBed.configureTestingModule({ imports: [CommunityGallery], providers: [
      provideHttpClient(), provideHttpClientTesting(), provideRouter([])
    ] }).overrideComponent(CommunityGallery, { set: { template: '', imports: [] } }).compileComponents();
    const fixture = TestBed.createComponent(CommunityGallery);
    const http = TestBed.inject(HttpTestingController);
    fixture.componentRef.setInput('groupId', 2); fixture.detectChanges();
    const album = { id: 1, title: 'Álbum grande', description: '', imageCount: 100, coverImageId: null };
    http.expectOne('/api/communities/2/albums').flush([album]);
    fixture.componentInstance['open'](album);
    http.expectOne('/api/communities/2/albums/1/images').flush(Array.from({ length: 100 }, (_, i) =>
      ({ id: i + 1, name: 'Foto ' + i, commentCount: 0, likeCount: 0, liked: false })));
    expect(fixture.componentInstance['visibleImages']().length).toBe(24);
    fixture.componentInstance['imageLimit'].update(fixture.componentInstance['addImagePage']);
    expect(fixture.componentInstance['visibleImages']().length).toBe(48);
    http.expectNone(request => /images\/\d+$/.test(request.url));
    fixture.componentInstance['closeAlbum']();
    expect(fixture.componentInstance['visibleImages']()).toEqual([]);
    http.verify();
  });

  it('opens an album selected by a search result after loading its metadata', async () => {
    const params = convertToParamMap({ album: '1' });
    const scroll = Object.getOwnPropertyDescriptor(HTMLElement.prototype, 'scrollIntoView');
    Object.defineProperty(HTMLElement.prototype, 'scrollIntoView', { configurable: true, value: vi.fn() });
    try {
      await TestBed.configureTestingModule({
        imports: [CommunityGallery],
        providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]),
          { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: params }, queryParamMap: of(params) } }]
      }).compileComponents();
      const fixture = TestBed.createComponent(CommunityGallery);
      const http = TestBed.inject(HttpTestingController);
      fixture.componentRef.setInput('groupId', 2);
      fixture.detectChanges();
      http.expectOne('/api/communities/2/albums').flush([
        { id: 1, title: 'Álbum encontrado', description: '', imageCount: 0, coverImageId: null }
      ]);
      http.expectOne('/api/communities/2/albums/1/images').flush([]);
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('.album-detail').textContent).toContain('Álbum encontrado');
      http.verify();
    } finally {
      if (scroll) Object.defineProperty(HTMLElement.prototype, 'scrollIntoView', scroll);
      else delete (HTMLElement.prototype as Partial<HTMLElement>).scrollIntoView;
    }
  });

  it.each([false, true])('uploads photos with optional historical year = %s', async historical => {
    await TestBed.configureTestingModule({
      imports: [CommunityGallery],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    }).compileComponents();
    const http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(CommunityGallery);
    fixture.componentRef.setInput('groupId', 2);
    fixture.componentRef.setInput('canManage', true);
    fixture.detectChanges();
    http.expectOne('/api/communities/2/albums').flush([
      { id: 1, title: 'Encuentro', description: '', imageCount: 0, coverImageId: null }
    ]);
    fixture.detectChanges();
    fixture.nativeElement.querySelector('.album-tile').click();
    http.expectOne('/api/communities/2/albums/1/images').flush([]);
    fixture.detectChanges();
    if (historical) {
      fixture.nativeElement.querySelector('.photo-upload-options').open = true;
      fixture.nativeElement.querySelector('.mat-mdc-select-trigger').click();
      fixture.detectChanges();
      const option = Array.from(document.querySelectorAll<HTMLElement>('mat-option'))
        .find(option => option.textContent?.includes('Solo sé el año'))!;
      option.click();
      fixture.detectChanges();
      const year = fixture.nativeElement.querySelector('input[type="number"]') as HTMLInputElement;
      year.value = '2016';
      year.dispatchEvent(new Event('input'));
      fixture.detectChanges();
    }
    const input = fixture.nativeElement.querySelector('input[type="file"]') as HTMLInputElement;
    Object.defineProperty(input, 'files', { configurable: true,
      value: [new File(['photo'], 'foto.png', { type: 'image/png' })] });
    input.dispatchEvent(new Event('change'));
    const request = http.expectOne({ method: 'POST', url: '/api/communities/2/albums/1/images' });
    const body = request.request.body as FormData;
    expect(body.get('takenYear')).toBe(historical ? '2016' : null);
    expect(body.get('takenOn')).toBeNull();
    request.flush({ id: 3, name: 'foto.png', commentCount: 0, likeCount: 0, liked: false });
    http.expectOne('/api/communities/2/albums/1/images').flush([]);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Imagen añadida al álbum.');
    http.verify();
  });
});

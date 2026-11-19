import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { provideRouter } from '@angular/router';
import { Subject } from 'rxjs';
import { AuthService } from '../../services/auth';
import { SavedItem } from '../../models/saved-item.model';
import { SavedItemsPageComponent } from './saved-items-page';

describe('SavedItemsPageComponent', () => {
  const post: SavedItem = {
    kind: 'POST', targetId: 3, groupId: 2, groupName: 'Comunidad',
    albumId: null, title: 'Guía', excerpt: 'Una publicación útil', savedAt: '2026-11-18T10:00:00Z'
  };
  let http: HttpTestingController;
  const closed = new Subject<void>();
  const dialog = { open: vi.fn((_component: unknown, _config: { data: unknown }) =>
    ({ afterClosed: () => closed.asObservable() })) };

  beforeEach(async () => {
    dialog.open.mockClear();
    await TestBed.configureTestingModule({
      imports: [SavedItemsPageComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]),
        { provide: AuthService, useValue: { userEmail: signal('me@example.cl') } },
        { provide: MatDialog, useValue: dialog }]
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    vi.unstubAllGlobals();
  });

  function start(items: SavedItem[], page = 0, total = items.length) {
    const fixture = TestBed.createComponent(SavedItemsPageComponent);
    fixture.detectChanges();
    http.expectOne('/api/saved-items?kind=ALL&page=0').flush({ items, total, page, size: 20 });
    fixture.detectChanges();
    if (items.length) {
      http.expectOne('/api/saved-items/state?groupId=2').flush({
        postIds: items.filter(item => item.kind === 'POST').map(item => item.targetId),
        imageIds: items.filter(item => item.kind === 'IMAGE').map(item => item.targetId),
        eventIds: items.filter(item => item.kind === 'EVENT').map(item => item.targetId)
      });
      fixture.detectChanges();
    }
    return fixture;
  }

  it('links to the saved publication and reloads the list after removing it', () => {
    const fixture = start([post]);
    const root = fixture.nativeElement as HTMLElement;
    const link = root.querySelector<HTMLAnchorElement>('.item-actions a')!;
    expect(link.getAttribute('href')).toContain('/grupos/2?seccion=posts&publicacion=3#publicacion-3');
    root.querySelector<HTMLButtonElement>('app-save-item-button button')!.click();
    http.expectOne({ method: 'DELETE', url: '/api/saved-items/POST/3' }).flush(null);
    http.expectOne('/api/saved-items?kind=ALL&page=0').flush({ items: [], total: 0, page: 0, size: 20 });
    fixture.detectChanges();
    expect(root.textContent).toContain('Aún no tienes guardados');
  });

  it('returns to the previous page when removing the last item on a page', () => {
    const fixture = start([post], 1, 21);
    fixture.nativeElement.querySelector('app-save-item-button button').click();
    http.expectOne({ method: 'DELETE', url: '/api/saved-items/POST/3' }).flush(null);
    http.expectOne('/api/saved-items?kind=ALL&page=1').flush({ items: [], total: 20, page: 1, size: 20 });
    http.expectOne('/api/saved-items?kind=ALL&page=0').flush({
      items: [{ ...post, targetId: 4 }], total: 20, page: 0, size: 20
    });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Guía');
  });

  it('filters events, links to the original event and removes its bookmark', () => {
    const event: SavedItem = { ...post, kind: 'EVENT', title: 'Encuentro anual' };
    const fixture = start([event]);
    const root: HTMLElement = fixture.nativeElement;
    expect(root.querySelector('.item-actions a')?.getAttribute('href'))
      .toContain('/grupos/2?seccion=events&evento=3#evento-3');
    expect(root.querySelector('.photo-preview')).toBeNull();
    Array.from(root.querySelectorAll<HTMLButtonElement>('.filters button'))
      .find(button => button.textContent?.trim() === 'Eventos')!.click();
    http.expectOne('/api/saved-items?kind=EVENT&page=0').flush({ items: [event], total: 1, page: 0, size: 20 });
    fixture.detectChanges();
    root.querySelector<HTMLButtonElement>('app-save-item-button button')!.click();
    http.expectOne({ method: 'DELETE', url: '/api/saved-items/EVENT/3' }).flush(null);
    http.expectOne('/api/saved-items?kind=EVENT&page=0').flush({ items: [], total: 0, page: 0, size: 20 });
    fixture.detectChanges();
    expect(root.textContent).toContain('No tienes guardados de este tipo');
  });

  it('opens the saved photo with its existing gallery viewer', () => {
    vi.stubGlobal('URL', class extends URL {
      static override createObjectURL = vi.fn(() => 'blob:saved-photo');
      static override revokeObjectURL = vi.fn();
    });
    const image: SavedItem = { ...post, kind: 'IMAGE', albumId: 5, title: 'Foto.png', excerpt: 'Encuentro' };
    const fixture = start([image]);
    http.expectOne('/api/communities/2/albums/5/images/3/thumbnail').flush(new Blob(['photo']));
    fixture.detectChanges();
    fixture.nativeElement.querySelector('.photo-preview').click();
    http.expectNone('/api/communities/2/albums/5/images/3');
    http.expectOne('/api/communities/2/albums/5/images/3/details').flush(
      { id: 3, name: 'Foto.png', likeCount: 2, liked: false, commentCount: 1 }
    );
    expect(dialog.open.mock.calls[0][1].data).toMatchObject({
      groupId: 2, initialIndex: 0, albumTitle: 'Encuentro',
      images: [{ id: 3, likeCount: 2 }]
    });
    closed.next();
    http.expectOne('/api/saved-items?kind=ALL&page=0').flush({ items: [], total: 0, page: 0, size: 20 });
  });
});

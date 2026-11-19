import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { CommunityMemories } from './community-memories';
import { MatDialog } from '@angular/material/dialog';
import { Subject } from 'rxjs';

describe('CommunityMemories', () => {
  it('uses a thumbnail and opens one photo without requesting the full album', async () => {
    const closed = new Subject<void>();
    const dialog = { open: vi.fn(() => ({ afterClosed: () => closed.asObservable() })) };
    await TestBed.configureTestingModule({ imports: [CommunityMemories], providers: [
      provideHttpClient(), provideHttpClientTesting(), { provide: MatDialog, useValue: dialog }
    ] }).compileComponents();
    const http = TestBed.inject(HttpTestingController);
    const create = vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:thumb-memory');
    const revoke = vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => {});
    try {
      const fixture = TestBed.createComponent(CommunityMemories);
      fixture.componentRef.setInput('groupId', 2); fixture.detectChanges();
      http.expectOne('/api/communities/2/memories?anniversariesOnly=false&page=0').flush({
        items: [{ imageId: 3, albumId: 1, name: 'Recuerdo', albumTitle: 'Historia',
          date: { uploadedOn: '2026-01-01', takenOn: null, takenYear: 2016, years: 10, anniversary: false, canEdit: false } }],
        total: 1, page: 0, size: 20
      });
      fixture.detectChanges();
      http.expectOne('/api/communities/2/albums/1/images/3/thumbnail').flush(new Blob());
      fixture.nativeElement.querySelector('button.photo').click();
      http.expectOne('/api/communities/2/albums/1/images/3/details').flush({ id: 3, name: 'Recuerdo', liked: false, likeCount: 0 });
      http.expectNone('/api/communities/2/albums/1/images');
      http.expectNone('/api/communities/2/albums/1/images/3');
      expect(dialog.open.mock.calls.length).toBe(1);
      fixture.destroy(); http.verify();
    } finally { create.mockRestore(); revoke.mockRestore(); }
  });

  it('loads the group memories and switches the anniversary filter', async () => {
    await TestBed.configureTestingModule({
      imports: [CommunityMemories], providers: [provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();
    const fixture = TestBed.createComponent(CommunityMemories);
    const http = TestBed.inject(HttpTestingController);
    fixture.componentRef.setInput('groupId', 2);
    fixture.detectChanges();
    http.expectOne('/api/communities/2/memories?anniversariesOnly=false&page=0')
      .flush({ items: [], total: 0, page: 0, size: 20 });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Tus recuerdos aparecerán aquí');
    fixture.nativeElement.querySelectorAll('.filters button')[1].click();
    http.expectOne('/api/communities/2/memories?anniversariesOnly=true&page=0')
      .flush({ items: [], total: 0, page: 0, size: 20 });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Hoy no hay aniversarios de fotos');
    http.verify();
  });
});

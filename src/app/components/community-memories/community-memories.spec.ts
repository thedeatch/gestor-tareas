import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { CommunityMemories } from './community-memories';

describe('CommunityMemories', () => {
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

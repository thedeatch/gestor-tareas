import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { DeferBlockBehavior, DeferBlockState, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { of } from 'rxjs';
import { CommunityDetailPage } from './community-detail-page';

describe('Community sections deferred loading', () => {
  it('renders a directly linked goals section after its code loads', async () => {
    await TestBed.configureTestingModule({ imports: [CommunityDetailPage], deferBlockBehavior: DeferBlockBehavior.Manual,
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]),
        { provide: MatDialog, useValue: {} },
        { provide: ActivatedRoute, useValue: {
          paramMap: of(convertToParamMap({ id: '2' })),
          queryParamMap: of(convertToParamMap({ seccion: 'goals' })),
          snapshot: { paramMap: convertToParamMap({ id: '2' }) }
        } }
      ] }).compileComponents();
    const http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(CommunityDetailPage);
    http.expectOne('/api/communities/2').flush({
      id: 2, name: 'Comunidad', members: [], currentUserRole: 'MEMBER', visibility: 'PRIVATE',
      hasImage: false, description: '', extendedDescription: '', rules: ''
    });
    http.expectOne('/api/communities/2/posts').flush([]);
    fixture.detectChanges();
    http.expectNone('/api/communities/2/goals');
    expect(fixture.nativeElement.textContent).toContain('Cargando objetivos');
    const blocks = await fixture.getDeferBlocks();
    expect(blocks.length).toBe(1);
    await blocks[0].render(DeferBlockState.Complete);
    http.expectOne('/api/communities/2/goals').flush({
      goals: [], tasks: [], canManage: false
    });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Aún no hay objetivos compartidos');
    http.expectNone(request => request.url.includes('/albums') || request.url.includes('/polls'));
    http.verify();
  });
});

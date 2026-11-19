import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { BehaviorSubject, of } from 'rxjs';
import { CommunityDetailPage } from './community-detail-page';

describe('CommunityDetailPage navigation', () => {
  let http: HttpTestingController;
  let params: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  beforeEach(async () => {
    params = new BehaviorSubject(convertToParamMap({ id: '2' }));
    await TestBed.configureTestingModule({ imports: [CommunityDetailPage], providers: [
      provideHttpClient(), provideHttpClientTesting(), provideRouter([]),
      { provide: ActivatedRoute, useValue: {
        paramMap: params, queryParamMap: of(convertToParamMap({})),
        snapshot: { paramMap: convertToParamMap({ id: '2' }) }
      } },
      { provide: MatDialog, useValue: {} }, { provide: MatSnackBar, useValue: { open: vi.fn() } }
    ] }).overrideComponent(CommunityDetailPage, { set: { template: '', imports: [] } }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  const group = (id: number) => ({ id, name: 'Grupo ' + id, members: [], currentUserRole: 'MEMBER', hasImage: false });

  it('cancels old group requests when navigating directly to another group', () => {
    const fixture = TestBed.createComponent(CommunityDetailPage);
    const old = http.expectOne('/api/communities/2');
    params.next(convertToParamMap({ id: '3' }));
    expect(old.cancelled).toBe(true);
    http.expectOne('/api/communities/3').flush(group(3));
    http.expectOne('/api/communities/3/posts').flush([]);
    expect(fixture.componentInstance['community']()?.id).toBe(3);
  });
  it('clears a failed load on retry and cancels outstanding image requests on exit', () => {
    const fixture = TestBed.createComponent(CommunityDetailPage);
    http.expectOne('/api/communities/2').flush({}, { status: 500, statusText: 'Error' });
    expect(fixture.componentInstance['error']()).toBeTruthy();
    fixture.componentInstance['load']();
    http.expectOne('/api/communities/2').flush({ ...group(2), hasImage: true });
    http.expectOne('/api/communities/2/posts').flush([]);
    const avatar = http.expectOne('/api/communities/2/image');
    expect(fixture.componentInstance['error']()).toBeNull();
    fixture.destroy(); expect(avatar.cancelled).toBe(true);
  });

  it('does not apply an old publication response to a newly opened group', () => {
    const fixture = TestBed.createComponent(CommunityDetailPage);
    http.expectOne('/api/communities/2').flush(group(2));
    http.expectOne('/api/communities/2/posts').flush([]);
    fixture.componentInstance['postTitle'].setValue('Novedad');
    fixture.componentInstance['postContent'].setValue('Contenido');
    fixture.componentInstance['publish']();
    const publication = http.expectOne('/api/communities/2/posts');
    params.next(convertToParamMap({ id: '3' }));
    expect(publication.cancelled).toBe(true);
    http.expectOne('/api/communities/3').flush(group(3));
    http.expectOne('/api/communities/3/posts').flush([]);
    expect(fixture.componentInstance['posts']()).toEqual([]);
    expect(fixture.componentInstance['savingPost']()).toBe(false);
  });
});

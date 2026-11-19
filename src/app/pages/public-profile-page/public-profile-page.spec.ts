import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { BehaviorSubject, of } from 'rxjs';
import { ModuleConfigurationService } from '../../services/module-configuration';
import { PublicProfilePage } from './public-profile-page';

describe('PublicProfilePage navigation', () => {
  let http: HttpTestingController;
  let params: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  beforeEach(async () => {
    params = new BehaviorSubject(convertToParamMap({ id: '3' }));
    await TestBed.configureTestingModule({ imports: [PublicProfilePage], providers: [
      provideHttpClient(), provideHttpClientTesting(), provideRouter([]),
      { provide: ActivatedRoute, useValue: { paramMap: params } },
      { provide: ModuleConfigurationService, useValue: { load: () => of({ messaging: true }) } }
    ] }).overrideComponent(PublicProfilePage, { set: { template: '', imports: [] } }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('loads the new profile and cancels an outdated response', () => {
    const fixture = TestBed.createComponent(PublicProfilePage);
    const previous = http.expectOne('/api/users/3/profile');
    params.next(convertToParamMap({ id: '4' }));
    expect(previous.cancelled).toBe(true);
    http.expectOne('/api/users/4/profile').flush({ id: 4, name: 'Ana', hasAvatar: false });
    http.expectOne('/api/users/me/blocks/4').flush({ canBlock: true, blocked: false });
    expect(fixture.componentInstance['profile']()?.id).toBe(4);
  });
  it('handles an unavailable avatar and cancels requests on exit', () => {
    const fixture = TestBed.createComponent(PublicProfilePage);
    http.expectOne('/api/users/3/profile').flush({ id: 3, name: 'Ana', hasAvatar: true });
    http.expectOne('/api/users/me/blocks/3').flush({ canBlock: true, blocked: false });
    http.expectOne('/api/users/3/avatar').flush(null, { status: 404, statusText: 'Not Found' });
    expect(fixture.componentInstance['avatarUrl']()).toBeNull();
    params.next(convertToParamMap({ id: '4' }));
    const pending = http.expectOne('/api/users/4/profile');
    fixture.destroy(); expect(pending.cancelled).toBe(true);
  });
});

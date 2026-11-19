import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { UserAvatar } from './user-avatar';

describe('UserAvatar', () => {
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [UserAvatar], providers: [
      provideHttpClient(withInterceptors([(request, next) => next(request.clone({ setHeaders: { Authorization: 'Bearer test' } }))])),
      provideHttpClientTesting()
    ] });
    http = TestBed.inject(HttpTestingController);
    vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:user-avatar');
    vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined);
  });
  afterEach(() => { http.verify(); vi.restoreAllMocks(); });
  function create() {
    const fixture = TestBed.createComponent(UserAvatar);
    fixture.componentRef.setInput('userId', 3);
    fixture.componentRef.setInput('hasAvatar', true);
    fixture.detectChanges();
    return fixture;
  }
  it('loads through HttpClient and releases the image URL on destruction', () => {
    const fixture = create();
    const request = http.expectOne('/api/users/3/avatar');
    expect(request.request.headers.get('Authorization')).toBe('Bearer test');
    expect(request.request.responseType).toBe('blob');
    request.flush(new Blob(['avatar'])); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('img').getAttribute('src')).toBe('blob:user-avatar');
    fixture.destroy(); expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:user-avatar');
  });
  it('cancels outdated requests and falls back gracefully on unavailable photos', () => {
    const fixture = create();
    const previous = http.expectOne('/api/users/3/avatar');
    fixture.componentRef.setInput('userId', 4); fixture.detectChanges();
    expect(previous.cancelled).toBe(true);
    http.expectOne('/api/users/4/avatar').flush(null, { status: 404, statusText: 'Not Found' });
    fixture.detectChanges(); expect(fixture.nativeElement.querySelector('img')).toBeNull();
    expect(fixture.nativeElement.querySelector('mat-icon')).toBeTruthy();
  });
});

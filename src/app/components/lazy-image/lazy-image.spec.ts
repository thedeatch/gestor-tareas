import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { LazyImage } from './lazy-image';

describe('LazyImage', () => {
  afterEach(() => vi.unstubAllGlobals());
  it('waits until visible, cancels old requests and releases its object URL', () => {
    const callbacks: ((entries: { isIntersecting: boolean }[]) => void)[] = [];
    vi.stubGlobal('IntersectionObserver', class {
      constructor(callback: (entries: { isIntersecting: boolean }[]) => void) { callbacks.push(callback); }
      observe() {} disconnect() {}
    });
    const revoke = vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => {});
    const create = vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:thumb');
    TestBed.configureTestingModule({ imports: [LazyImage], providers: [provideHttpClient(), provideHttpClientTesting()] });
    const http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(LazyImage);
    fixture.componentRef.setInput('src', '/thumb/1'); fixture.detectChanges();
    http.expectNone('/thumb/1');
    callbacks[0]([{ isIntersecting: true }]);
    const old = http.expectOne('/thumb/1');
    fixture.componentRef.setInput('src', '/thumb/2'); fixture.detectChanges();
    expect(old.cancelled).toBe(true);
    callbacks[1]([{ isIntersecting: true }]);
    http.expectOne('/thumb/2').flush(new Blob()); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('img').src).toBe('blob:thumb');
    fixture.destroy(); expect(revoke).toHaveBeenCalledWith('blob:thumb');
    http.verify(); create.mockRestore(); revoke.mockRestore();
  });
});

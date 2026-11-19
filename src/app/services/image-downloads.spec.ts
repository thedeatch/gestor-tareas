import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ImageDownloads } from './image-downloads';

describe('ImageDownloads', () => {
  it('limits downloads to four and starts the next after completion or cancellation', () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const http = TestBed.inject(HttpTestingController);
    const service = TestBed.inject(ImageDownloads);
    const subscriptions = Array.from({ length: 6 }, (_, i) => service.get('/thumb/' + i).subscribe());
    const initial = http.match(request => request.url.startsWith('/thumb/'));
    expect(initial.length).toBe(4);
    initial[0].flush(new Blob());
    const fifth = http.expectOne('/thumb/4');
    subscriptions[5].unsubscribe();
    subscriptions[1].unsubscribe();
    expect(initial[1].cancelled).toBe(true);
    http.expectNone('/thumb/5');
    initial[2].flush(new Blob()); initial[3].flush(new Blob()); fifth.flush(new Blob());
    http.verify();
  });
});

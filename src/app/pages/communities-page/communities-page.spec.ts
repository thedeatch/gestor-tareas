import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { CommunitiesPage } from './communities-page';

describe('CommunitiesPage', () => {
  it('should load the current user communities', async () => {
    TestBed.configureTestingModule({
      imports: [CommunitiesPage],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    const fixture = TestBed.createComponent(CommunitiesPage);
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/communities').flush([]);
    http.expectOne('/api/communities/discover').flush([]);
    http.expectOne('/api/communities/invitations').flush([]);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.componentInstance).toBeTruthy();
    expect((fixture.nativeElement as HTMLElement).textContent)
      .toContain('Aún no perteneces a un grupo');
    http.verify();
  });
});

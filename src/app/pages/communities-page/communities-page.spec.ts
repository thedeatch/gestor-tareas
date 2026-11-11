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

  it('should offer accepting an invitation instead of requesting access', async () => {
    TestBed.configureTestingModule({
      imports: [CommunitiesPage],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    });

    const fixture = TestBed.createComponent(CommunitiesPage);
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/communities').flush([]);
    http.expectOne('/api/communities/discover').flush([{
      id: 7, name: 'Grupo privado', slug: 'grupo-privado',
      description: 'Acceso por invitación', hasImage: false,
      memberCount: 1, requestStatus: null
    }]);
    http.expectOne('/api/communities/invitations').flush([{
      id: 21, communityId: 7, communityName: 'Grupo privado',
      communityHasImage: false, userId: 2,
      userName: 'Invitada', userEmail: 'invitada@example.com',
      type: 'INVITATION', status: 'PENDING', createdAt: new Date().toISOString()
    }]);
    await fixture.whenStable();
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Te invitaron a este grupo');
    expect(text).toContain('Aceptar');
    expect(text).not.toContain('Solicitar ingreso');
    http.verify();
  });
});

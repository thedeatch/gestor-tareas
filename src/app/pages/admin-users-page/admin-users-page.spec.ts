import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { AdminUsersPage } from './admin-users-page';

describe('AdminUsersPage', () => {
  it('should create', async () => {
    await TestBed.configureTestingModule({
      imports: [AdminUsersPage],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();

    const fixture = TestBed.createComponent(AdminUsersPage);
    expect(fixture.componentInstance).toBeTruthy();
  });
});

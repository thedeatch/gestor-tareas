import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { NotificationsPage } from './notifications-page';

describe('NotificationsPage', () => {
  it('should create', () => {
    TestBed.configureTestingModule({
      imports: [NotificationsPage],
      providers: [provideHttpClient(), provideRouter([])]
    });

    const fixture = TestBed.createComponent(NotificationsPage);
    expect(fixture.componentInstance).toBeTruthy();
  });
});
